package dev.patrickgold.florisboard.app.setup

/**
 * The [NotificationPermissionState] is used to determine the status of the notification permission.
 * The default value is [NOT_SET].
 * This value is only updated to [GRANTED] or [DENIED] on android 13+, depending on what the user selects.
 */
enum class NotificationPermissionState {
    NOT_SET,
    GRANTED,
    DENIED;
}
