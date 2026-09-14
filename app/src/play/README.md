# Play distribution services

`DistributionUpdates` queries Google Play when Settings opens and displays update availability with a link to the store. Failure, unknown status and a 10-second timeout never appear as “up to date”. DEV builds display an explanation because their package ID is not the release listing.

Play App Update 2.1.0 and its dependencies are restricted to `playImplementation`. No permissions are added. See [third-party notices](../../../THIRD_PARTY_LICENSES.md).
