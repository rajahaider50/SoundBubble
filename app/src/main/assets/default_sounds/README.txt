Place your default (built-in) audio files inside this folder, for example:

app/src/main/assets/default_sounds/
    hello.mp3
    laugh.mp3
    taunt1.mp3
    taunt2.mp3

Note:
- Only .mp3 / .wav / .m4a / .ogg / .aac / .3gp / .amr formats are supported
- You may keep or delete this file (README.txt); it is not an audio file so it will be ignored automatically
- Make sure this folder is included in the commit when pushing to GitHub Actions
- When the app is opened for the first time, these files are automatically copied to every user's audio list
