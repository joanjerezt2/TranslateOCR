package mt.browser.bergamot

class NativeLib {

    /**
     * A native method that is implemented by the 'bergamot' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'bergamot' library on application startup.
        init {
            System.loadLibrary("bergamot")
        }
    }
}