import 'package:flutter/scheduler.dart';
import 'package:flutter/widgets.dart';

/* Dispose after the current frame so TextField / TextFormField can detach. */
void disposeTextController(TextEditingController? controller) {
  if (controller == null) return;
  final c = controller;
  SchedulerBinding.instance.addPostFrameCallback((_) {
    c.dispose();
  });
}

void disposeTextControllers(Iterable<TextEditingController?> controllers) {
  for (final c in controllers) {
    disposeTextController(c);
  }
}
