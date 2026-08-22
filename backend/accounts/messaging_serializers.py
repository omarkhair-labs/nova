"""Compatibility alias for the Phase 6 Messaging serializer owner."""

import sys

from .messaging import messaging_serializers as _implementation

sys.modules[__name__] = _implementation
