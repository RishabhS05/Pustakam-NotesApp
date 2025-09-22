import shared

extension String {
    func toLocalFormat(showTime: Bool = true) -> String {
        let timeZone = Kotlinx_datetimeTimeZone.companion.currentSystemDefault()
        return String_extKt.toLocalFormat(self, timeZone: timeZone, showTime: showTime)
    }
}
