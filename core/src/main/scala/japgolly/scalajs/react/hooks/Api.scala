package japgolly.scalajs.react.hooks

import japgolly.scalajs.react.{CallbackTo, Reusability}
import Hooks._

object Api {

  trait Step {
    type Next[A]
  }

  trait SubsequentStep[_Ctx, _CtxFn[_]] extends Step {
    final type Ctx = _Ctx
    final type CtxFn[A] = _CtxFn[A]
    def squash[A]: CtxFn[A] => (Ctx => A)
  }

  final class Var[A](initialValue: A) {
    var value: A =
      initialValue
  }

  // ===================================================================================================================
  // API 1: X / (Ctx => X)

  trait Primary[Ctx, _Step <: Step] {
    final type Step = _Step

    protected def next[H](f: Ctx => H)(implicit step: Step): step.Next[H]

    /** Provides you with a means to do whatever you want without the static guarantees that the normal DSL provides.
      * It's up to you to ensure you don't vioalte React's hook rules.
      */
    final def unchecked[A](f: Ctx => A)(implicit step: Step): step.Next[A] =
      next(f)

    /** Create a new `var` on each render. */
    final def newVar[A](f: Ctx => A)(implicit step: Step): step.Next[Var[A]] =
      next(ctx => new Var(f(ctx)))

    /** Create a new `val` on each render. */
    final def newVal[A](f: Ctx => A)(implicit step: Step): step.Next[A] =
      next(f)

    /** Create a new `lazy val` on each render. */
    final def newLazyVal[A](f: Ctx => A)(implicit step: Step): step.Next[() => A] =
      next { ctx =>
        lazy val a: A = f(ctx)
        val result = () => a // TODO: Report Scala bug
        result
      }

    /** Use a custom hook */
    final def custom[I, O](hook: CustomHook[I, O])(implicit step: Step, a: CustomHook.Arg[Ctx, I]): step.Next[O] =
      next(ctx => hook.unsafeInit(a.convert(ctx)))

    /** Use a custom hook */
    final def custom[O](hook: Ctx => CustomHook[Unit, O])(implicit step: Step): step.Next[O] =
      next(hook(_).unsafeInit(()))

    /** Returns a stateful value, and a function to update it.
      *
      * During the initial render, the returned state is the same as the value passed as the first argument
      * (initialState).
      *
      * During subsequent re-renders, the first value returned by useState will always be the most recent state after
      * applying updates.
      */
    final def useState[S](initialState: Ctx => S)(implicit step: Step): step.Next[UseState[S]] =
      next(ctx => UseState.unsafeCreate(initialState(ctx)))

    /** An alternative to [[useState]]. Accepts a reducer of type `(state, action) => newState`, and returns the
      * current state paired with a dispatch method.
      * (If you’re familiar with Redux, you already know how this works.)
      *
      * useReducer is usually preferable to useState when you have complex state logic that involves multiple
      * sub-values or when the next state depends on the previous one. useReducer also lets you optimize performance
      * for components that trigger deep updates because you can pass dispatch down instead of callbacks.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#usereducer
      */
    final def useReducer[S, A](reducer: (S, A) => S, initialArg: S)(implicit step: Step): step.Next[UseReducer[S, A]] =
      next(_ => UseReducer.unsafeCreate(reducer, initialArg))

    /** An alternative to [[useState]]. Accepts a reducer of type `(state, action) => newState`, and returns the
      * current state paired with a dispatch method.
      * (If you’re familiar with Redux, you already know how this works.)
      *
      * useReducer is usually preferable to useState when you have complex state logic that involves multiple
      * sub-values or when the next state depends on the previous one. useReducer also lets you optimize performance
      * for components that trigger deep updates because you can pass dispatch down instead of callbacks.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#usereducer
      */
    final def useReducer[I, S, A](reducer: (S, A) => S, initialArg: I, init: I => S)(implicit step: Step): step.Next[UseReducer[S, A]] =
      next(_ => UseReducer.unsafeCreate(reducer, initialArg, init))

    /** An alternative to [[useState]]. Accepts a reducer of type `(state, action) => newState`, and returns the
      * current state paired with a dispatch method.
      * (If you’re familiar with Redux, you already know how this works.)
      *
      * useReducer is usually preferable to useState when you have complex state logic that involves multiple
      * sub-values or when the next state depends on the previous one. useReducer also lets you optimize performance
      * for components that trigger deep updates because you can pass dispatch down instead of callbacks.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#usereducer
      */
    final def useReducer[S, A](init: Ctx => UseReducerInline => HookCreated[UseReducer[S, A]])(implicit step: Step): step.Next[UseReducer[S, A]] =
      next(init(_)(new UseReducerInline).result)

    /** The callback passed to useEffect will run after the render is committed to the screen. Think of effects as an
      * escape hatch from React’s purely functional world into the imperative world.
      *
      * By default, effects run after every completed render.
      * If you'd only like to execute the effect when your component is mounted, then use [[useEffectOnMount]].
      * If you'd only like to execute the effect when certain values have changed, provide those certain values as
      * the second argument.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useeffect
      */
    final def useEffect[A](effect: CallbackTo[A])(implicit a: EffectArg[A], step: Step): step.Next[Unit] =
      next(_ => UseEffect.unsafeCreate(effect))

    /** The callback passed to useEffect will run after the render is committed to the screen. Think of effects as an
      * escape hatch from React’s purely functional world into the imperative world.
      *
      * This will only execute the effect when values in the second argument, change.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useeffect
      */
    final def useEffect[A, D](effect: CallbackTo[A], deps: D)(implicit a: EffectArg[A], r: Reusability[D], step: Step): step.Next[Unit] =
      custom(ReusableEffect.useEffect(effect, deps))

    /** The callback passed to useEffect will run after the render is committed to the screen. Think of effects as an
      * escape hatch from React’s purely functional world into the imperative world.
      *
      * By default, effects run after every completed render.
      * If you'd only like to execute the effect when your component is mounted, then use [[useEffectOnMount]].
      * If you'd only like to execute the effect when certain values have changed, provide those certain values as
      * the second argument.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useeffect
      */
    final def useEffect(init: Ctx => UseEffectInline => HookCreated[Unit])(implicit step: Step): step.Next[Unit] =
      next(init(_)(new UseEffectInline).result)

    /** The callback passed to useEffect will run after the render is committed to the screen. Think of effects as an
      * escape hatch from React’s purely functional world into the imperative world.
      *
      * This will only execute the effect when your component is mounted.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useeffect
      */
    final def useEffectOnMount[A](effect: CallbackTo[A])(implicit a: EffectArg[A], step: Step): step.Next[Unit] =
      useEffectOnMount((_: Ctx) => effect)

    /** The callback passed to useEffect will run after the render is committed to the screen. Think of effects as an
      * escape hatch from React’s purely functional world into the imperative world.
      *
      * This will only execute the effect when your component is mounted.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useeffect
      */
    final def useEffectOnMount[A](effect: Ctx => CallbackTo[A])(implicit a: EffectArg[A], step: Step): step.Next[Unit] =
      next(ctx => UseEffect.unsafeCreateOnMount(effect(ctx)))

    /** The signature is identical to [[useEffect]], but it fires synchronously after all DOM mutations. Use this to
      * read layout from the DOM and synchronously re-render. Updates scheduled inside useLayoutEffect will be flushed
      * synchronously, before the browser has a chance to paint.
      *
      * Prefer the standard [[useEffect]] when possible to avoid blocking visual updates.
      *
      * If you'd only like to execute the effect when your component is mounted, then use [[useLayoutEffectOnMount]].
      * If you'd only like to execute the effect when certain values have changed, provide those certain values as
      * the second argument.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useLayoutEffect
      */
    final def useLayoutEffect[A](effect: CallbackTo[A])(implicit a: EffectArg[A], step: Step): step.Next[Unit] =
      next(_ => UseLayoutEffect.unsafeCreate(effect))

    /** The signature is identical to [[useEffect]], but it fires synchronously after all DOM mutations. Use this to
      * read layout from the DOM and synchronously re-render. Updates scheduled inside useLayoutEffect will be flushed
      * synchronously, before the browser has a chance to paint.
      *
      * Prefer the standard [[useEffect]] when possible to avoid blocking visual updates.
      *
      * This will only execute the effect when values in the second argument, change.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useLayoutEffect
      */
    final def useLayoutEffect[A, D](effect: CallbackTo[A], deps: D)(implicit a: EffectArg[A], r: Reusability[D], step: Step): step.Next[Unit] =
      custom(ReusableEffect.useLayoutEffect(effect, deps))

    /** The signature is identical to [[useEffect]], but it fires synchronously after all DOM mutations. Use this to
      * read layout from the DOM and synchronously re-render. Updates scheduled inside useLayoutEffect will be flushed
      * synchronously, before the browser has a chance to paint.
      *
      * Prefer the standard [[useEffect]] when possible to avoid blocking visual updates.
      *
      * If you'd only like to execute the effect when your component is mounted, then use [[useLayoutEffectOnMount]].
      * If you'd only like to execute the effect when certain values have changed, provide those certain values as
      * the second argument.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useLayoutEffect
      */
    final def useLayoutEffect(init: Ctx => UseLayoutEffectInline => HookCreated[Unit])(implicit step: Step): step.Next[Unit] =
      next(init(_)(new UseLayoutEffectInline).result)

    /** The signature is identical to [[useEffect]], but it fires synchronously after all DOM mutations. Use this to
      * read layout from the DOM and synchronously re-render. Updates scheduled inside useLayoutEffect will be flushed
      * synchronously, before the browser has a chance to paint.
      *
      * Prefer the standard [[useEffect]] when possible to avoid blocking visual updates.
      *
      * This will only execute the effect when your component is mounted.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useLayoutEffect
      */
    final def useLayoutEffectOnMount[A](effect: CallbackTo[A])(implicit a: EffectArg[A], step: Step): step.Next[Unit] =
      useLayoutEffectOnMount((_: Ctx) => effect)

    /** The signature is identical to [[useEffect]], but it fires synchronously after all DOM mutations. Use this to
      * read layout from the DOM and synchronously re-render. Updates scheduled inside useLayoutEffect will be flushed
      * synchronously, before the browser has a chance to paint.
      *
      * Prefer the standard [[useEffect]] when possible to avoid blocking visual updates.
      *
      * This will only execute the effect when your component is mounted.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useLayoutEffect
      */
    final def useLayoutEffectOnMount[A](effect: Ctx => CallbackTo[A])(implicit a: EffectArg[A], step: Step): step.Next[Unit] =
      next(ctx => UseLayoutEffect.unsafeCreateOnMount(effect(ctx)))

  }

  // ===================================================================================================================
  // API 2: (H1, H2, ..., Hn) => X

  trait Secondary[Ctx, CtxFn[_], _Step <: SubsequentStep[Ctx, CtxFn]] extends Primary[Ctx, _Step] {

    /** Provides you with a means to do whatever you want without the static guarantees that the normal DSL provides.
      * It's up to you to ensure you don't vioalte React's hook rules.
      */
    final def unchecked[A](f: CtxFn[A])(implicit step: Step): step.Next[A] =
      unchecked(step.squash(f)(_))

    /** Create a new `var` on each render. */
    final def newVar[A](f: CtxFn[A])(implicit step: Step): step.Next[Var[A]] =
      newVar(step.squash(f)(_))

    /** Create a new `val` on each render. */
    final def newVal[A](f: CtxFn[A])(implicit step: Step): step.Next[A] =
      newVal(step.squash(f)(_))

    /** Create a new `lazy val` on each render. */
    final def newLazyVal[A](f: CtxFn[A])(implicit step: Step): step.Next[() => A] =
      newLazyVal(step.squash(f)(_))

    /** Use a custom hook */
    final def custom[O](hook: CtxFn[CustomHook[Unit, O]])(implicit step: Step): step.Next[O] =
      custom(step.squash(hook)(_))

    /** Returns a stateful value, and a function to update it.
      *
      * During the initial render, the returned state is the same as the value passed as the first argument
      * (initialState).
      *
      * During subsequent re-renders, the first value returned by useState will always be the most recent state after
      * applying updates.
      */
    final def useState[S](initialState: CtxFn[S])(implicit step: Step): step.Next[UseState[S]] =
      useState(step.squash(initialState)(_))

    /** An alternative to [[useState]]. Accepts a reducer of type `(state, action) => newState`, and returns the
      * current state paired with a dispatch method.
      * (If you’re familiar with Redux, you already know how this works.)
      *
      * useReducer is usually preferable to useState when you have complex state logic that involves multiple
      * sub-values or when the next state depends on the previous one. useReducer also lets you optimize performance
      * for components that trigger deep updates because you can pass dispatch down instead of callbacks.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#usereducer
      */
    final def useReducer[S, A](init: CtxFn[UseReducerInline => HookCreated[UseReducer[S, A]]])(implicit step: Step): step.Next[UseReducer[S, A]] =
      useReducer(step.squash(init)(_))

    /** The callback passed to useEffect will run after the render is committed to the screen. Think of effects as an
      * escape hatch from React’s purely functional world into the imperative world.
      *
      * By default, effects run after every completed render.
      * If you'd only like to execute the effect when your component is mounted, then use [[useEffectOnMount]].
      * If you'd only like to execute the effect when certain values have changed, provide those certain values as
      * the second argument.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useeffect
      */
    final def useEffect(init: CtxFn[UseEffectInline => HookCreated[Unit]])(implicit step: Step): step.Next[Unit] =
      useEffect(step.squash(init)(_))

    /** The callback passed to useEffect will run after the render is committed to the screen. Think of effects as an
      * escape hatch from React’s purely functional world into the imperative world.
      *
      * This will only execute the effect when your component is mounted.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useeffect
      */
    final def useEffectOnMount[A](effect: CtxFn[CallbackTo[A]])(implicit a: EffectArg[A], step: Step): step.Next[Unit] =
      useEffectOnMount(step.squash(effect)(_))

    /** The signature is identical to [[useEffect]], but it fires synchronously after all DOM mutations. Use this to
      * read layout from the DOM and synchronously re-render. Updates scheduled inside useLayoutEffect will be flushed
      * synchronously, before the browser has a chance to paint.
      *
      * Prefer the standard [[useEffect]] when possible to avoid blocking visual updates.
      *
      * If you'd only like to execute the effect when your component is mounted, then use [[useLayoutEffectOnMount]].
      * If you'd only like to execute the effect when certain values have changed, provide those certain values as
      * the second argument.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useLayoutEffect
      */
    final def useLayoutEffect(init: CtxFn[UseLayoutEffectInline => HookCreated[Unit]])(implicit step: Step): step.Next[Unit] =
      useLayoutEffect(step.squash(init)(_))

    /** The signature is identical to [[useEffect]], but it fires synchronously after all DOM mutations. Use this to
      * read layout from the DOM and synchronously re-render. Updates scheduled inside useLayoutEffect will be flushed
      * synchronously, before the browser has a chance to paint.
      *
      * Prefer the standard [[useEffect]] when possible to avoid blocking visual updates.
      *
      * This will only execute the effect when your component is mounted.
      *
      * @see https://reactjs.org/docs/hooks-reference.html#useLayoutEffect
      */
    final def useLayoutEffectOnMount[A](effect: CtxFn[CallbackTo[A]])(implicit a: EffectArg[A], step: Step): step.Next[Unit] =
      useLayoutEffectOnMount(step.squash(effect)(_))
  }

  // ===================================================================================================================
  // Inline API

  final class HookCreated[A] private[Api] (val result: A)

  sealed trait Inline {
    private var calls = 0
    protected def wrap[A](a: A): HookCreated[A] = {
      if (calls > 0)
        throw new RuntimeException("Hook already created.")
      calls += 1
      new HookCreated(a)
    }
  }

  final class UseReducerInline extends Inline {
    def apply[S, A](reducer: (S, A) => S, initialArg: S): HookCreated[UseReducer[S, A]] =
      wrap(UseReducer.unsafeCreate(reducer, initialArg))

    def apply[I, S, A](reducer: (S, A) => S, initialArg: I, init: I => S): HookCreated[UseReducer[S, A]] =
      wrap(UseReducer.unsafeCreate(reducer, initialArg, init))
  }

  final class UseEffectInline extends Inline {
    def apply[A](effect: CallbackTo[A])(implicit a: EffectArg[A]): HookCreated[Unit] =
      wrap(UseEffect.unsafeCreate(effect))

    def apply[A, D](effect: CallbackTo[A], deps: D)(implicit a: EffectArg[A], r: Reusability[D]): HookCreated[Unit] =
      wrap(ReusableEffect.useEffect(effect, deps).unsafeInit(()))
  }

  final class UseLayoutEffectInline extends Inline {
    def apply[A](effect: CallbackTo[A])(implicit a: EffectArg[A]): HookCreated[Unit] =
      wrap(UseLayoutEffect.unsafeCreate(effect))

    def apply[A, D](effect: CallbackTo[A], deps: D)(implicit a: EffectArg[A], r: Reusability[D]): HookCreated[Unit] =
      wrap(ReusableEffect.useLayoutEffect(effect, deps).unsafeInit(()))
  }

}
