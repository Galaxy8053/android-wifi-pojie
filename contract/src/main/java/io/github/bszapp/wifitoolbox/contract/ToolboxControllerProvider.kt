package io.github.bszapp.wifitoolbox.contract

object ToolboxControllerProvider {
    private var controller: IToolboxController? = null

    fun register(c: IToolboxController) { controller = c }

    fun get(): IToolboxController = controller
        ?: error("IToolboxController 未注册，请在 Application.onCreate 中调用 register()")
}