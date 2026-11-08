# Phoenix Client (Minecraft 1.21.11, Fabric)

A ClickGUI utility client laid out like the reference screenshot: 8 category panels, module search,
and a bottom tab bar (Modules / HUD / Friends / Theme / Config).

Needs: Fabric Loader 0.18+, Fabric API for 1.21.11, Java 21. Works alongside Sodium (nothing here touches the
world renderer: ESP-style modules are drawn as 2D HUD graphics, Glow ESP uses the vanilla outline).

## Getting the jar (no installs): GitHub Actions
1. Create a new GitHub repository.
2. Upload everything in this folder, including the hidden `.github` folder.
   (If `.github` does not upload, create the file `.github/workflows/build.yml` in the GitHub web editor and paste in `build.yml` from this zip.)
3. Open the repo's **Actions** tab -> "Build Phoenix Client" -> wait for the green tick.
4. Open the run, download the **phoenix-client-jar** artifact, unzip it, and put `phoenix-client-1.0.0.jar` in your `mods` folder.

## Building locally
Install JDK 21, then run `./gradlew build` (Windows: `gradlew build`). The jar is `build/libs/phoenix-client-1.0.0.jar`.

## Using it
* **Right Shift** opens the GUI (rebindable in Controls -> Phoenix).
* Left click a module = toggle. Right click = settings / bind / hold mode. Middle click = bind.
* Drag a panel header to move it. Right click a header to collapse it. Scroll long panels.
* Config is saved to `config/phoenix.json`.

## Status
Modules that do nothing yet show a chat message when you click them ("not implemented yet").
Everything else is implemented. Requests to expand it: send the build error or the module name.
