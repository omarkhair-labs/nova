# Pulse backend ownership

This package owns Pulse HTTP routes, visibility, serialization, and creation policy. The durable Django model remains registered under the existing `accounts` app through `accounts.pulse_models` so database/app identity stays consistent with Nova's modular-monolith architecture.
