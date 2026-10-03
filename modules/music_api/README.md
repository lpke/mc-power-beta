# ZastavkaAPI for Minecraft Beta 1.7.3

A mod for Minecraft Beta 1.7.3 that provides an API for freezing entities, getting currently playing sound, and other data.
(Zastávka is the Czech word for station/stop similar to StationAPI, a suggested play on words by the modding community :D)
* Mod is multiplayer compatible

### Features (client and server)
* Ability to get/set enemies frozen time (in Minecraft ticks)
* Ability to get entity block collision variable (used to dynamically change block collision for different entities)
* Fixes some falling block entities with StationAPI (simple full cubes, mostly)
  * Temporary until StationAPI figures out how to handle blocks that are not full cubes that use the same texture for each side

### Clientside Features
* Ability to get currently playing streaming song or music song
* Ability to make music biome or dimension specific
* Ability to cancel current background music

## Installation using Prism Launcher

1. Download an instance of Babric for Prism Launcher: https://github.com/Glass-Series/babric-prism-instance
2. Install Java 17 and set the instance to use it: https://adoptium.net/temurin/releases/
3. (Optional) Add Mod Menu to the mod folder for the instance: https://modrinth.com/mod/modmenu-beta
4. Add this mod to the mod folder for the instance: https://github.com/telvarost/ZastavkaAPI/releases
5. Run and enjoy! 👍

## Feedback

Got any suggestions on what should be added next? Feel free to share it by [creating an issue](https://github.com/telvarost/ZastavkaAPI/issues/new). Know how to code and want to do it yourself? Then look below on how to get started.

## Contributing

Thanks for considering contributing! To get started fork this repository, make your changes, and create a PR. 

If you are new to StationAPI consider watching the following videos on Babric/StationAPI Minecraft modding: https://www.youtube.com/watch?v=9-sVGjnGJ5s&list=PLa2JWzyvH63wGcj5-i0P12VkJG7PDyo9T
