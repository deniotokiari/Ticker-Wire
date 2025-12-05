package pl.deniotokiari.tickerwire.common.platform

/**
 * Platform-specific URL launcher interface.
 * Opens URLs in the default browser or a new tab (for web platforms).
 */
expect fun openUrl(url: String)

