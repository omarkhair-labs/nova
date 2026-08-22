"""Compatibility alias for the Phase 6 Social paging implementation owner."""

import sys

from .social import paging as _implementation

sys.modules[__name__] = _implementation
