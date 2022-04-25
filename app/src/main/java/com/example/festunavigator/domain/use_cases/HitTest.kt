package com.example.festunavigator.domain.use_cases

import com.example.festunavigator.data.model.DetectedObjectResult
import com.example.festunavigator.domain.hit_test.OrientatedPosition
import com.google.ar.core.Frame
import com.google.ar.sceneform.collision.Ray
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.collision.pickHitTest
import io.github.sceneview.math.*

class HitTest {

    /**
     * A [HitTest] converts 2d frame coordinates into 3d world coordinates with a plane alignment using rays.
     * @property targetPos the object coordinates on the frame
     * @property cameraPos the camera world position. The ray casting starting point
     */
      operator fun invoke(frame: Frame, targetPos: Float2, cameraPos: Float3, sceneView: ArSceneView): Result<OrientatedPosition> {

            val hitResult1 = frame.hitTest(targetPos.x, targetPos.y)
            val hitResult2 = frame.hitTest(targetPos.x-5, targetPos.y)
            val hitResult3 = frame.hitTest(targetPos.x, targetPos.y+5)

            if (hitResult1.isNotEmpty() && hitResult2.isNotEmpty() && hitResult3.isNotEmpty() ) {
                val result1 = hitResult1.first()
                val result2 = hitResult2.first()
                val result3 = hitResult3.first()

                val pos1 = Vector3(
                    result1.hitPose.tx(),
                    result1.hitPose.ty(),
                    result1.hitPose.tz()
                )

                val pos2 = Vector3(
                    result2.hitPose.tx(),
                    result2.hitPose.ty(),
                    result2.hitPose.tz()
                )

                val pos3 = Vector3(
                    result3.hitPose.tx(),
                    result3.hitPose.ty(),
                    result3.hitPose.tz()
                )

                val vector1 = Vector3.subtract(pos1, pos2).normalized()
                val vector2 = Vector3.subtract(pos1, pos3).normalized()

                val vectorForward = Vector3.cross(vector1, vector2).normalized()

                val orientation = Quaternion.lookRotation(
                    vectorForward,
                    Vector3.up()
                ).toNewQuaternion()

                return Result.success(OrientatedPosition(pos1.toFloat3(), orientation, result1))
            }
            else {
                return Result.failure(Exception("Null hit result"))
            }
    }

}