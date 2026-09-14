import 'package:flutter/material.dart';

class ThreeDotsLoader extends StatefulWidget {
  const ThreeDotsLoader({
    super.key,
    this.color = Colors.white,
    this.dotSize = 7,
    this.spacing = 5,
  });

  final Color color;
  final double dotSize;
  final double spacing;

  @override
  State<ThreeDotsLoader> createState() => ThreeDotsLoaderState();
}

class ThreeDotsLoaderState extends State<ThreeDotsLoader>
    with SingleTickerProviderStateMixin {
  late final AnimationController controller;

  @override
  void initState() {
    super.initState();
    controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    )..repeat();
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  double bounceOffset(double t) {
    /* Smooth up-down bounce per dot cycle. */
    return -5 * (t < 0.5 ? t * 2 : (1 - t) * 2);
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 22,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: List.generate(3, (index) {
          return AnimatedBuilder(
            animation: controller,
            builder: (context, child) {
              final phase = (controller.value - index * 0.18) % 1.0;
              final offsetY = bounceOffset(phase < 0 ? phase + 1 : phase);
              return Transform.translate(
                offset: Offset(0, offsetY),
                child: child,
              );
            },
            child: Container(
              width: widget.dotSize,
              height: widget.dotSize,
              margin: EdgeInsets.symmetric(horizontal: widget.spacing / 2),
              decoration: BoxDecoration(
                color: widget.color,
                shape: BoxShape.circle,
              ),
            ),
          );
        }),
      ),
    );
  }
}
