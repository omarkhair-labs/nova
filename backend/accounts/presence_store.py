import asyncio
import os
import time

try:
    from redis.asyncio import Redis
except ImportError:  # pragma: no cover - local fallback covers installs without Redis.
    Redis = None


LEASE_SECONDS = 90
KEY_TTL_SECONDS = LEASE_SECONDS + 120
REDIS_URL = os.getenv("REDIS_URL", "").strip()
KEY_PREFIX = os.getenv("NOVA_PRESENCE_PREFIX", "nova:presence").strip() or "nova:presence"

_redis_client = None
_redis_loop = None
_local_presence = {}
_local_lock = asyncio.Lock()


def redis_enabled():
    return bool(REDIS_URL and Redis is not None)


def _key(user_id):
    return f"{KEY_PREFIX}:user:{int(user_id)}"


def _client():
    global _redis_client, _redis_loop
    if not redis_enabled():
        return None

    # Daphne normally has one long-lived event loop per worker. Django async
    # tests may create a fresh loop per test, so never reuse an asyncio Redis
    # connection pool across different loops.
    loop = asyncio.get_running_loop()
    if _redis_client is None or _redis_loop is not loop:
        _redis_loop = loop
        _redis_client = Redis.from_url(
            REDIS_URL,
            decode_responses=True,
            socket_connect_timeout=5,
            socket_timeout=5,
            health_check_interval=30,
        )
    return _redis_client


def _now():
    return int(time.time())


async def register_lease(user_id, lease_id):
    """Register one active app/socket lease and return (became_online, active_count)."""

    client = _client()
    if client is not None:
        now = _now()
        key = _key(user_id)
        async with client.pipeline(transaction=True) as pipe:
            pipe.zremrangebyscore(key, "-inf", now)
            pipe.zcard(key)
            pipe.zadd(key, {lease_id: now + LEASE_SECONDS})
            pipe.expire(key, KEY_TTL_SECONDS)
            results = await pipe.execute()
        previous_count = int(results[1])
        return previous_count == 0, previous_count + 1

    async with _local_lock:
        leases = _local_presence.setdefault(int(user_id), set())
        was_empty = not leases
        leases.add(lease_id)
        return was_empty, len(leases)


async def refresh_lease(user_id, lease_id):
    client = _client()
    if client is not None:
        now = _now()
        key = _key(user_id)
        async with client.pipeline(transaction=True) as pipe:
            pipe.zremrangebyscore(key, "-inf", now)
            pipe.zadd(key, {lease_id: now + LEASE_SECONDS})
            pipe.expire(key, KEY_TTL_SECONDS)
            await pipe.execute()
        return

    async with _local_lock:
        _local_presence.setdefault(int(user_id), set()).add(lease_id)


async def unregister_lease(user_id, lease_id):
    """Remove one lease and return the number of still-active leases."""

    client = _client()
    if client is not None:
        now = _now()
        key = _key(user_id)
        async with client.pipeline(transaction=True) as pipe:
            pipe.zrem(key, lease_id)
            pipe.zremrangebyscore(key, "-inf", now)
            pipe.zcard(key)
            results = await pipe.execute()
        return int(results[2])

    async with _local_lock:
        leases = _local_presence.get(int(user_id))
        if not leases:
            return 0
        leases.discard(lease_id)
        if not leases:
            _local_presence.pop(int(user_id), None)
            return 0
        return len(leases)


async def is_online(user_id):
    client = _client()
    if client is not None:
        now = _now()
        key = _key(user_id)
        async with client.pipeline(transaction=True) as pipe:
            pipe.zremrangebyscore(key, "-inf", now)
            pipe.zcard(key)
            results = await pipe.execute()
        return int(results[1]) > 0

    async with _local_lock:
        return bool(_local_presence.get(int(user_id)))


async def active_count(user_id):
    client = _client()
    if client is not None:
        now = _now()
        key = _key(user_id)
        async with client.pipeline(transaction=True) as pipe:
            pipe.zremrangebyscore(key, "-inf", now)
            pipe.zcard(key)
            results = await pipe.execute()
        return int(results[1])

    async with _local_lock:
        return len(_local_presence.get(int(user_id), set()))
