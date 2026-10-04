import 'package:flutter/material.dart';

class ZoomControls extends StatelessWidget {
  final double currentZoom;
  final double minZoom;
  final double maxZoom;
  final ValueChanged<double> onZoomChanged;

  const ZoomControls({
    super.key,
    required this.currentZoom,
    required this.minZoom,
    required this.maxZoom,
    required this.onZoomChanged,
  });

  @override
  Widget build(BuildContext context) {
    final presets = [0.5, 1.0, 2.0, 3.0];

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // Preset Buttons
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: presets.map((preset) {
            final targetZoom = preset.clamp(minZoom, maxZoom);
            final isSelected = (currentZoom - targetZoom).abs() < 0.15;

            return Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4.0),
              child: ChoiceChip(
                label: Text('${preset}x'),
                selected: isSelected,
                selectedColor: Colors.amberAccent,
                labelStyle: TextStyle(
                  color: isSelected ? Colors.black : Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 12,
                ),
                backgroundColor: Colors.black54,
                onSelected: (_) => onZoomChanged(targetZoom),
              ),
            );
          }).toList(),
        ),

        const SizedBox(height: 4),

        // Zoom Slider
        if (maxZoom > minZoom)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24.0),
            child: Row(
              children: [
                const Icon(Icons.zoom_out, color: Colors.white70, size: 18),
                Expanded(
                  child: SliderTheme(
                    data: SliderTheme.of(context).copyWith(
                      trackHeight: 2,
                      thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
                      overlayShape: const RoundSliderOverlayShape(overlayRadius: 12),
                      activeTrackColor: Colors.amberAccent,
                      inactiveTrackColor: Colors.white24,
                      thumbColor: Colors.amberAccent,
                    ),
                    child: Slider(
                      value: currentZoom.clamp(minZoom, maxZoom),
                      min: minZoom,
                      max: maxZoom,
                      onChanged: onZoomChanged,
                    ),
                  ),
                ),
                const Icon(Icons.zoom_in, color: Colors.white70, size: 18),
              ],
            ),
          ),
      ],
    );
  }
}
