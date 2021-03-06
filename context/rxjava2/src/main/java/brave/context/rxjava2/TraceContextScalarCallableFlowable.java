package brave.context.rxjava2;

import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.Scope;
import brave.propagation.TraceContext;
import io.reactivex.Flowable;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.fuseable.ScalarCallable;
import org.reactivestreams.Publisher;

final class TraceContextScalarCallableFlowable<T> extends Flowable<T> implements ScalarCallable<T> {
  final Publisher<T> source;
  final CurrentTraceContext currentTraceContext;
  final TraceContext assemblyContext;

  TraceContextScalarCallableFlowable(
      Publisher<T> source, CurrentTraceContext currentTraceContext, TraceContext assemblyContext) {
    this.source = source;
    this.currentTraceContext = currentTraceContext;
    this.assemblyContext = assemblyContext;
  }

  @Override
  protected void subscribeActual(org.reactivestreams.Subscriber<? super T> s) {
    try (Scope scope = currentTraceContext.maybeScope(assemblyContext)) {
      if (s instanceof ConditionalSubscriber) {
        source.subscribe(
            new TraceContextConditionalSubscriber<>(
                (ConditionalSubscriber) s, currentTraceContext, assemblyContext));
      } else {
        source.subscribe(new TraceContextSubscriber<>(s, currentTraceContext, assemblyContext));
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public T call() {
    try (Scope scope = currentTraceContext.maybeScope(assemblyContext)) {
      return ((ScalarCallable<T>) source).call();
    }
  }
}
