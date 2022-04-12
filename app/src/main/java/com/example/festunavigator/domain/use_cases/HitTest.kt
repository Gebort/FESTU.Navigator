package com.example.festunavigator.domain.use_cases

import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.toNewQuaternion

class HitTest {

    suspend operator fun invoke(surfaceView: ArSceneView): Result<OrientatedPosition> {

            val result1 = surfaceView.arSession?.currentFrame?.hitTest(surfaceView.width/2f, surfaceView.height/2f)
            val result2 = surfaceView.arSession?.currentFrame?.hitTest(surfaceView.width/2f-5, surfaceView.height/2f)
            val result3 = surfaceView.arSession?.currentFrame?.hitTest(surfaceView.width/2f, surfaceView.height/2f+5)
            if (result1 != null && result2 != null && result3 != null) {
                val startDistance = surfaceView.camera.worldPosition
                val translation1 = result1.hitPose.translation
                val pos1 = Position(
                    startDistance.x + translation1[0],
                    startDistance.y + translation1[1],
                    startDistance.z + translation1[2]
                )

                val translation2 = result2.hitPose.translation
                val pos2 = Position(
                    startDistance.x + translation2[0],
                    startDistance.y + translation2[1],
                    startDistance.z + translation2[2]
                )

                val translation3 = result3.hitPose.translation
                val pos3 = Position(
                    startDistance.x + translation3[0],
                    startDistance.y + translation3[1],
                    startDistance.z + translation3[2]
                )

                val vector1 = Vector3().apply {
                    x = pos1.x - pos2.x
                    y = pos1.y - pos2.y
                    z = pos1.z - pos2.z
                    normalized()
                }
                val vector2 = Vector3().apply {
                    x = pos1.x - pos3.x
                    y = pos1.y - pos3.y
                    z = pos1.z - pos3.z
                    normalized()
                }

                val vectorForward = Vector3.cross(vector1, vector2).normalized()

                val orientation = Quaternion.lookRotation(
                    vectorForward,
                    vector2
                ).toNewQuaternion()

                return Result.success(OrientatedPosition(pos1, orientation))
            }
            else {
                return Result.failure(Exception("Null hit result"))
            }
    }

}