package demo

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
// import scala.scalajs.js.annotation._

object Counter {
  // type Props = Unit
  // type State = Int

  // final class Backend($: BackendScope[Props, State]) {
  //   def render(s: State): VdomNode =
  // }

  val Component = {
    val sig = ReactRefresh.sig()
    org.scalajs.dom.console.log("Sig: ", sig)
    val Component = ScalaFnComponent.withHooks[Unit]
      .localVal(sig(): Unit)
      .useState(0)
      .renderWithReuse { (_, _, s) =>

        <.button(
          "Count xxx: ", s.value,
          ^.onClick --> s.modState(_ + 1),
        )
      }
    sig(Component.raw, "oDgYfYHkD9Wkv4hrAPCkI/ev3YU=")
    ReactRefresh.reg(Component.raw, "Counter")
    Component
  }

  org.scalajs.dom.console.log("Counter.Component: ", Component.raw)
}

// object Counter {
//   type Props = Unit
//   type State = Int

//   final class Backend($: BackendScope[Props, State]) {
//     def render(s: State): VdomNode =
//       <.button(
//         "Count: ", s,
//         ^.onClick --> $.modState(_ + 1),
//       )
//   }

//   val Component = ScalaComponent.builder[Props]
//     .initialState(0)
//     .renderBackend[Backend]
//     .configure(Reusability.shouldComponentUpdate)
//     .build
// }