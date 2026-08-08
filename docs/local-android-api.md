# Local Android ↔ Django development

Nova's debug Android build talks to the Django API through `http://127.0.0.1:8000/api/v1/`.

Because the app is tested on a physical Android device over USB, forward the phone's port 8000 to the development PC before running the app:

```powershell
adb reverse tcp:8000 tcp:8000
```

Then keep the backend running from the repository root:

```powershell
docker compose up
```

The debug manifest allows cleartext HTTP only for local development. Release builds do not opt into cleartext traffic.

Useful checks:

```powershell
adb reverse --list
```

The expected mapping is:

```text
tcp:8000 tcp:8000
```

The API health endpoint on the PC is:

```text
http://localhost:8000/api/v1/health/
```
