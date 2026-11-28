import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'dart:typed_data';

typedef Data = Map<String, dynamic>;
Data object(Object? value) => value is Map<String, dynamic> ? value : <String, dynamic>{};
List<Data> rows(Object? value) => value is List
    ? value.whereType<Map<String, dynamic>>().toList()
    : <Data>[];
String field(Data data, String key, [String fallback = '']) =>
    data[key] is String && (data[key] as String).trim().isNotEmpty
        ? data[key] as String : fallback;
String uuid() {
  final b = List<int>.generate(16, (_) => Random.secure().nextInt(256));
  b[6] = (b[6] & 15) | 64;
  b[8] = (b[8] & 63) | 128;
  final s = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${s.substring(0,8)}-${s.substring(8,12)}-${s.substring(12,16)}-${s.substring(16,20)}-${s.substring(20)}';
}
String dateOnly(String value) => value.length >= 10 ? value.substring(0, 10) : value;
String localDay() {
  final d = DateTime.now();
  return '${d.year}-${d.month.toString().padLeft(2,'0')}-${d.day.toString().padLeft(2,'0')}';
}

class ApiProblem implements Exception {
  final int status;
  final String message;
  const ApiProblem(this.status, this.message);
  @override
  String toString() => message;
}
abstract interface class Gateway {
  Future<Data> login(String email, String password);
  Future<Data> get(String path);
  Future<Data> post(String path, Data body);
  Future<Uint8List?> picture(String reference, {bool catalog = false});
  void clear();
}

class OracleReviewGateway implements Gateway {
  // Compile-time configuration, not a user-entered arbitrary credential destination.
  // Deliberately no production fallback. The only authorized origin is the
  // existing review service supplied by the build.
  final Uri origin;
  final HttpClient _http = HttpClient()..connectionTimeout = const Duration(seconds: 15);
  String _access = '', _refresh = '';
  int _epoch = 0;
  Future<void>? _renewing;
  OracleReviewGateway(String base) : origin = Uri.tryParse(base) ?? Uri();

  bool get configured => origin.scheme == 'https' && origin.host.isNotEmpty &&
      origin.path == '/api/v1/' && origin.userInfo.isEmpty &&
      !origin.hasQuery && !origin.hasFragment;
  Uri _uri(String path) {
    if (!configured) throw const ApiProblem(0, 'This review build has no approved QA server configured.');
    final uri = origin.resolve(path);
    if (uri.origin != origin.origin || !uri.path.startsWith(origin.path) ||
        uri.userInfo.isNotEmpty || uri.hasFragment) {
      throw const ApiProblem(0, 'The requested address is outside the review service.');
    }
    return uri;
  }
  void _check(int epoch) {
    if (epoch != _epoch) throw const ApiProblem(401, 'This session has ended.');
  }
  Future<({int status, Uint8List bytes})> _exchange(Uri uri, String method,
      {Data? body, String? bearer, int maxBytes = 8 * 1024 * 1024}) async {
    final request = await _http.openUrl(method, uri).timeout(const Duration(seconds: 20));
    request.followRedirects = false;
    request.headers.set('Accept', 'application/json, image/*');
    if (bearer != null && bearer.isNotEmpty) request.headers.set('Authorization', 'Bearer $bearer');
    if (body != null) {
      request.headers.contentType = ContentType.json;
      request.write(jsonEncode(body));
    }
    final response = await request.close().timeout(const Duration(seconds: 30));
    if (response.contentLength > maxBytes) {
      throw const ApiProblem(0, 'The response is too large.');
    }
    final buffer = BytesBuilder(copy: false);
    await for (final chunk in response.timeout(const Duration(seconds: 25))) {
      if (buffer.length + chunk.length > maxBytes) {
        throw const ApiProblem(0, 'The response is too large.');
      }
      buffer.add(chunk);
    }
    return (status: response.statusCode, bytes: buffer.takeBytes());
  }
  Data _decode(({int status, Uint8List bytes}) response) {
    Data data = {};
    try { data = object(jsonDecode(utf8.decode(response.bytes))); }
    catch (_) {
      if (response.status >= 200 && response.status < 300) {
        throw const ApiProblem(0, 'The service returned an unexpected response.');
      }
    }
    if (response.status < 200 || response.status >= 300) {
      final safe = field(data, 'detail');
      throw ApiProblem(response.status, safe.isNotEmpty && safe.length <= 400
          ? safe : response.status == 401 ? 'Sign in again to continue.'
          : response.status == 403 ? 'This action is not available to your account.'
          : 'The request could not be completed. (${response.status})');
    }
    return data;
  }
  @override
  Future<Data> login(String email, String password) async {
    clear();
    final epoch = _epoch;
    final data = _decode(await _exchange(_uri('auth/login/'), 'POST', body: {
      'email': email.trim(), 'password': password,
      'device_name': 'Plantupia Flutter review', 'platform': 'android',
    }));
    _check(epoch);
    if (field(data,'access').isEmpty || field(data,'refresh').isEmpty ||
        object(data['user'])['id'] == null) {
      throw const ApiProblem(0, 'The service did not return a complete account session.');
    }
    _access = field(data,'access'); _refresh = field(data,'refresh');
    return object(data['user']);
  }
  Future<void> _renew(int epoch) async {
    if (_renewing != null) { await _renewing; _check(epoch); return; }
    if (_refresh.isEmpty) throw const ApiProblem(401, 'Sign in again to continue.');
    final refresh = _refresh;
    final future = () async {
      final data = _decode(await _exchange(_uri('auth/refresh/'), 'POST', body: {'refresh': refresh}));
      _check(epoch);
      final token = field(data,'access');
      if (token.isEmpty) throw const ApiProblem(401, 'Sign in again to continue.');
      _access = token;
      if (field(data,'refresh').isNotEmpty) _refresh = field(data,'refresh');
    }();
    _renewing = future;
    try { await future; } finally { if (identical(_renewing,future)) _renewing = null; }
  }
  Future<Data> _request(String method, String path, {Data? body}) async {
    final epoch = _epoch;
    final used = _access;
    if (used.isEmpty) throw const ApiProblem(401, 'Sign in again to continue.');
    var response = await _exchange(_uri(path), method, body: body, bearer: used);
    _check(epoch);
    // Only retry a rejected unauthorized request, never an ambiguous network write.
    if (response.status == 401) {
      if (used == _access) await _renew(epoch);
      _check(epoch);
      response = await _exchange(_uri(path), method, body: body, bearer: _access);
      _check(epoch);
    }
    return _decode(response);
  }
  @override
  Future<Data> get(String path) => _request('GET',path);
  @override
  Future<Data> post(String path, Data body) => _request('POST',path,body:body);
  @override
  Future<Uint8List?> picture(String reference, {bool catalog = false}) async {
    if (reference.isEmpty) return null;
    final epoch = _epoch;
    try {
      if (catalog) {
        final data = await get(reference);
        final encoded = field(data,'image_base64');
        _check(epoch);
        if (encoded.isEmpty || encoded.length > 6 * 1024 * 1024) return null;
        return base64Decode(encoded);
      }
      // Never forward a login token to a storage provider or redirect target.
      final uri = origin.resolve(reference);
      if (!configured || uri.scheme != 'https' || uri.userInfo.isNotEmpty || uri.hasFragment) return null;
      final same = uri.origin == origin.origin;
      final result = await _exchange(uri, 'GET', bearer: same ? _access : null,
          maxBytes: 10 * 1024 * 1024);
      _check(epoch);
      return result.status == 200 ? result.bytes : null;
    } catch (_) { return null; }
  }
  @override
  void clear() {
    _epoch++; _access = ''; _refresh = ''; _renewing = null;
  }
}
