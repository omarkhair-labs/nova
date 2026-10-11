# Plantupia / Care loop (PR #240)

## Product decision (working implementation, not clinical advice)
Planta and Greg provide real task and care flows; the original Plantupia discovery instead emphasized living context, a dated record, and observing consequences. The first bridging feature is a **manual check-in**:
- A signed-in owner or authorized carer can record a real observation, care act, or concern.
- They may optionally choose to check the same plant again in 7 or 14 days.
- This is a personal reflection date, **not** a predicted watering interval, sensor reading, reminder push notification, or plant diagnosis.
- The private plant list shows the earliest pending check-in date; the plant detail shows pending check-ins.
- Closing a check-in requires a new observed outcome with a real note; the source event is marked completed atomically and the outcome is linked to it.
- Viewers can read shared-space records but cannot close or schedule care; other users cannot read them.
- An event with no opted-in check-in is still valid. Public Circle sharing remains a separate consented action.

## Acceptance conditions
- New user can create a space, create a plant, log a care event, optionally select +7/+14 days, reopen the app, see pending date, return to the plant and record a real result.
- The date persists in staging database, does not leak in public Circle, and does not imply push delivery.
- Prior CI green is not physical-device proof. Test with two accounts and a real photo.
- No production deployment, Play release or merge from this draft PR.
