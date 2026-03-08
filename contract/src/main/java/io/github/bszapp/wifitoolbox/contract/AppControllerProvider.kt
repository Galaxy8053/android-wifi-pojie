package io.github.bszapp.wifitoolbox.contract

object AppControllerProvider {
    private var controller: IAppController? = null

    fun register(c: IAppController) {
        controller = c
    }

    fun get(): IAppController = controller!!
}