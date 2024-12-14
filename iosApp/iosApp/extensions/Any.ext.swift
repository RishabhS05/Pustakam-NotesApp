
extension Any? {
    func isNil() -> Bool {
        return self == nil
    }
}

extension AnyObject? {
func isNil () -> Bool {
        return self == nil
    }
}
extension String? {
    func isNotNilOrEmpty() -> Bool {
        return self != nil && !self!.isEmpty
    }
}
