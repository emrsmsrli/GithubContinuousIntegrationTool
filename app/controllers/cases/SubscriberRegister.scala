package controllers.cases

case class SubscriberRegister(username: String,
                              repository: String,
                              token: String) {
    def isValid: Boolean = username.isEmpty && repository.isEmpty && token.isEmpty
}