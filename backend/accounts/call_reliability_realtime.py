from channels.db import database_sync_to_async

from .call_realtime import CallConsumer
from .calls import clear_call_liveness
from .models import CallSession


class ReliableCallConsumer(CallConsumer):
    """Call socket with cleanup for a caller that leaves while still ringing."""

    async def disconnect(self, close_code):
        user = self.scope.get("user")
        should_clear = False
        if user and user.is_authenticated and getattr(self, "call_id", None):
            should_clear = await self._caller_is_leaving_ringing_call(user.pk)

        await super().disconnect(close_code)

        # The old implementation left the 90-second Redis lease behind even
        # when the outgoing CallActivity had already disappeared. Clear only a
        # ringing caller's lease; an active call or a callee reconnect stays safe.
        if should_clear:
            await database_sync_to_async(clear_call_liveness)(self.call_id)

    @database_sync_to_async
    def _caller_is_leaving_ringing_call(self, user_id):
        return CallSession.objects.filter(
            pk=self.call_id,
            caller_id=user_id,
            status=CallSession.Status.RINGING,
        ).exists()
