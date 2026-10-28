package com.nova.app.navigation

internal val plantupiaPrimaryRoutes = setOf(
    NovaRoute.Home, NovaRoute.Plants, NovaRoute.Create, NovaRoute.Shop, NovaRoute.Orbit,
)

/** Retain each primary entry's saveable state without retaining unrelated subroutes. */
internal fun plantupiaRootStack(current: List<NovaRoute>, destination: NovaRoute): List<NovaRoute> {
    if (destination == NovaRoute.Profile) return current.filterNot { it == NovaRoute.Profile } + destination
    require(destination in plantupiaPrimaryRoutes)
    val retained = current.filter { it in plantupiaPrimaryRoutes }.distinct()
    return retained.filterNot { it == destination } + destination
}
