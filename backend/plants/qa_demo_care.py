"""Source-backed sample content for isolated QA only.

NOT reviewed or approved by Plantupia. These entries intentionally carry
an explicit QA disclaimer in the visible Android guide text, and are never
installed by migrations, production deploys or normal application startup.
Paraphrases of RHS public growing guides, with links for a real reviewer.
"""

SAMPLE_CARE_GUIDES = (
    {
        "key": "qa-monstera-deliciosa",
        "scientific_name": "Monstera deliciosa",
        "source_url": "https://www.rhs.org.uk/plants/swiss-cheese-plants",
        "source_reference": "RHS Swiss cheese plants guide — SYNTHETIC QA ONLY",
        "instructions": (
            "SYNTHETIC QA DEMO — NOT APPROVED BY PLANTUPIA. "
            "Monstera generally prefers bright indirect light and warm surroundings. "
            "Inspect the compost before watering rather than using a fixed schedule. "
            "Avoid leaving the pot standing in water."
        ),
        "context_notes": (
            "Example only: confirm this specimen's identity, pot drainage, "
            "placement and actual condition with a qualified Plantupia reviewer."
        ),
    },
    {
        "key": "qa-chlorophytum-comosum",
        "scientific_name": "Chlorophytum comosum",
        "source_url": "https://www.rhs.org.uk/plants/spider-plants/growing-guide",
        "source_reference": "RHS Spider plants growing guide — SYNTHETIC QA ONLY",
        "instructions": (
            "SYNTHETIC QA DEMO — NOT APPROVED BY PLANTUPIA. "
            "Spider plants tolerate a range of indoor light, but protect them "
            "from harsh direct sunlight. Check compost moisture regularly, "
            "and do not allow the pot to remain in soggy compost."
        ),
        "context_notes": (
            "Example only: actual watering depends on this plant's substrate, "
            "container, season and placement; inspect before acting."
        ),
    },
    {
        "key": "qa-sansevieria-trifasciata",
        "scientific_name": "Sansevieria trifasciata",
        "source_url": "https://www.rhs.org.uk/plants/sansevieria/growing-guide",
        "source_reference": "RHS Sansevieria growing guide — SYNTHETIC QA ONLY",
        "instructions": (
            "SYNTHETIC QA DEMO — NOT APPROVED BY PLANTUPIA. "
            "Snake plants tolerate many indoor light conditions and need "
            "free-draining compost. Check that the compost has dried before "
            "considering watering; overwatering may damage the roots."
        ),
        "context_notes": (
            "Example only: do not infer moisture from elapsed time. "
            "The actual specimen and cultivar have not been professionally verified."
        ),
    },
)
