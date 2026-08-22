"""Compatibility alias for the Phase 6 Auth JWT implementation owner."""

import sys

from .auth import jwt_auth as _implementation

sys.modules[__name__] = _implementation
