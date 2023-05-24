# FESTU.Navigator


The project, developed for a FESTU university (city Khabarovsk) competition, implements AR navigation through the FESTU university buildings along a pre-created corridor graph. 
Work with AR is done entirely using the SceneView library

**Video demonstration available below**

The application builds routes between university classrooms. When launching the application, it is required to scan the number of the nearest classroom in order to set the starting point of the route. 
Classrooms number recognition performed using Google MLKit. 

**By default, the recognizable classroom number must be vertical and not contain letters in the name**


<p align="middle">
  <img src="https://user-images.githubusercontent.com/35885530/185281107-2485e7b1-2a59-4fce-9049-7830cd024d96.png" width="250" />
</p>

<br>

It is required to confirm the recognized classroom number and its position in 3D space, since an error is possible in determining the planes around the user

<p align="middle">
  <img src="https://user-images.githubusercontent.com/35885530/185281219-aa1f2617-9328-47fa-a175-f5563780730b.png" width="250" />
</p>

<br>

Initialization is required to determine the user's current position relative to the saved classrooms graph. After initialization, the user can select both the start and end points of the route

<p align="middle">
  <img src="https://user-images.githubusercontent.com/35885530/185281644-cd024077-782f-450b-818a-38546d7b6638.png" width="250" />
</p>

<br>

The built route thanks to the SceneView AR library is displayed in 3D space in front of the user. Finding the shortest route is done using the A* algorithm, path smoothing is done using Bezier curves. 

<p align="middle">
  <img src="https://user-images.githubusercontent.com/35885530/185281417-9d558881-1cf6-43cc-9e62-852645bbdcd8.jpeg" width="200" />
    <img src="https://user-images.githubusercontent.com/35885530/185281425-3fb933ed-e07c-4600-90cb-c6ed4d755526.jpg" width="200" />
</p>

<br>

The classrooms graph is created manually by the application administrator through a special interface

<p align="middle">
  <img src="https://user-images.githubusercontent.com/35885530/185282075-e17f1e4f-6ebb-43c0-a644-d8d591758ec0.png" width="250" />
</p>

<br>

## Video demonstration
https://user-images.githubusercontent.com/35885530/185282923-e424978b-de67-486c-a4b1-ebde455ae3ee.mp4

## Settings
You can change the path rendering distance (increase the number of nodes) and other settings, in festunavigator/presentation/preview/PreviewFragment.kt:
```kotlin
360 companion object {
361        //path rendering distance (number of nodes)
362        const val VIEWABLE_PATH_NODES = 21
363        //tree rendering distance, used only in admin mode
364        const val VIEWABLE_ADMIN_NODES = 5f
365        //how often the check for path and tree redraw will be
366        const val POSITION_DETECT_DELAY = 100L
367        //image crop for recognition
368        val DESIRED_CROP = Pair(8, 72)
369    }
```
You can change the classroom number template (first char must be a digit by standard) in festunavigator/data/ml/classification/TextAnalyzer.kt:
```kotlin
143    private fun filterFunction(text: Text.TextBlock): Boolean {
144        return text.text[0].isDigit()
145    }
```

## Admin/User mode
To enable the audience graph editing mode, change the build variant to adminDebug or adminRelease:

<p align="middle">
  <img src="https://user-images.githubusercontent.com/35885530/186618592-0728c71b-8d19-4874-89ae-0e3a8986c7d2.png" width="300" />
</p>

<br>

By default, the application is installed with a pre-installed FESTU university classrooms graph. To run the application without a pre-installed graph, in festunavigator/domain/di/ModuleApp.kt find:
```kotlin
32 return Room.databaseBuilder(this, GraphDatabase::class.java, DATABASE_NAME)
33     .createFromAsset(DATABASE_DIR)
34     .allowMainThreadQueries()
35     .addMigrations()
36     .build()
```
Remove line - .createFromAsset(DATABASE_DIR):
```kotlin
32 return Room.databaseBuilder(this, GraphDatabase::class.java, DATABASE_NAME)
33     .allowMainThreadQueries()
35     .addMigrations()
34     .build()
```

Now you can run the app and experience it anywhere

## Admin mode video demonstration

An example of setting up navigation in a new space is available here (the video is too long for github): https://drive.google.com/file/d/11a_lTeQmXhMfE2AxJ8mg5ec34yCC1uoH/view?usp=sharing




