# FotoCamera - Android Camera App

A professional Android camera application built with Kotlin that allows users to add customizable 4-digit number overlays to their photos.

## Features

🎯 **Set as Default Camera App** - Can be set as the device's default camera application

📷 **Camera Preview** - Real-time camera preview using Camera2 API

🔢 **Number Overlay** - Customizable 4-digit number displayed on viewfinder and saved photos
- First 2 digits: Red background
- Last 2 digits: Blue background
- Click to change via wheel picker dialog

💾 **Smart File Naming** - Photos saved with format: `Number-day.month-time` (e.g., `1234-28.02-11:48.jpg`)

🔄 **Persistent Settings** - Number preference saved and restored between app launches

## Screenshots

*Camera preview with number overlay in bottom-right corner*
*Number picker dialog with 4 individual digit wheels*

## Technical Details

- **Language**: Kotlin
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Camera API**: CameraX
- **Architecture**: MVVM pattern with ViewBinding

## Dependencies

- **CameraX**: Camera functionality and lifecycle management
- **Material Design**: Modern UI components
- **Number Picker**: Custom wheel picker for digit selection
- **SharedPreferences**: Persistent storage for user settings

## Permissions Required

- `CAMERA` - Access device camera
- `WRITE_EXTERNAL_STORAGE` - Save photos to device storage
- `READ_EXTERNAL_STORAGE` - Read existing photos

## Installation

1. Clone the repository
2. Open in Android Studio
3. Build and run on device or emulator
4. Grant camera and storage permissions when prompted

## Usage

1. **First Launch**: App asks to set as default camera
2. **Take Photos**: Use capture button in camera view
3. **Change Number**: Tap the number overlay to open picker dialog
4. **View Photos**: Photos saved to `Pictures/FotoCamera/` with overlay

## File Structure

```
app/src/main/
├── java/com/fotocammera/
│   ├── MainActivity.kt          # Main entry point and permissions
│   ├── CameraActivity.kt        # Camera preview and photo capture
│   ├── NumberPickerDialog.kt    # Number selection dialog
│   └── utils/
│       └── PreferencesManager.kt # Settings storage
├── res/
│   ├── layout/                  # UI layouts
│   ├── values/                  # Strings, colors, themes
│   └── drawable/                # Icons and graphics
└── AndroidManifest.xml          # App configuration and permissions
```

## Key Features Implementation

### Default Camera App
- Intent filters for `IMAGE_CAPTURE` and `CAMERA_BUTTON` actions
- Proper manifest configuration for camera app registration

### Number Overlay
- Real-time display on camera preview
- Canvas drawing on captured images
- Two-color background (red/blue) for visual distinction

### Wheel Picker Dialog
- 4 individual NumberPicker components
- Range 0-9 for each digit
- Smooth scrolling and selection

### Image Processing
- Post-capture overlay addition using Canvas
- High-quality JPEG compression
- Custom file naming with timestamp

## Future Enhancements

- [ ] Video recording with overlay
- [ ] Multiple overlay styles/colors
- [ ] Cloud backup integration
- [ ] Advanced camera settings
- [ ] Photo editing features

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, email support@fotocamera.com or create an issue in this repository.