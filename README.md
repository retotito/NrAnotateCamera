# FotoCamera - Android Camera App

A  Android camera application built with Kotlin that allows users to add customizable 4-digit number overlays to their photos.

## Features

📷 **Camera Preview** - Real-time camera preview using Camera2 API

🔢 **Number Overlay** - Customizable 4-digit number displayed on viewfinder and saved photos
- First 2 digits: Red background
- Last 2 digits: Blue background
- Click to change via wheel picker dialog

💾 **Smart File Naming** - Photos saved with format: `Number-day.month-time` (e.g., `1234-28.02-11.48.56.jpg`)

🔄 **Persistent Settings** - Number preference saved and restored between app launches

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

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, email support@fotocamera.com or create an issue in this repository.
