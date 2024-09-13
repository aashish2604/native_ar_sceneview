package com.example.ar_demo

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.math.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var arSceneView: ARSceneView
    private var modelNode: ModelNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arSceneView = findViewById<ARSceneView?>(R.id.arSceneView).apply {
            lifecycle = this@MainActivity.lifecycle
            configureSession { session, config ->
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            }
            onSessionUpdated = {_, frame ->
                if (modelNode != null) {
                    placeModelOnSurface(frame)
                }
            }
        }

        loadAndPlaceModel()

    }

    private fun loadAndPlaceModel() {
        CoroutineScope(Dispatchers.Main).launch {
            modelNode = withContext(Dispatchers.IO) {
                arSceneView.modelLoader.loadModelInstance("models/sofa_single.glb")?.let { modelInstance ->
                    ModelNode(
                        modelInstance = modelInstance,
                        scaleToUnits = 0.5f,  // Adjust scale as needed
                        // Position the model on the ground
                    ).apply {
                        isEditable = true
                    }
                }
            }
//            modelNode?.let {
//                arSceneView.addChildNode(it)
//            }


        }
    }

    private fun handleTouch(event: MotionEvent) {
        val hitResult = arSceneView.hitTestAR(event.x, event.y)
        hitResult?.let { hit ->
            modelNode?.let { node ->
                // Move the model to the touch position
                val newPose = hit.hitPose
                node.position = Position(
                    newPose.tx(),
                    newPose.ty(),
                    newPose.tz()
                )
            }
        }
    }

    private fun placeModelOnSurface(frame: Frame) {
        // Get updated planes from the frame
        val plane = frame.getUpdatedPlanes()
            .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }

        // If a suitable plane is detected
        plane?.let { detectedPlane ->
            // Create an anchor at the center of the detected plane
            val anchor = detectedPlane.createAnchor(detectedPlane.centerPose)

            // Place the modelNode on the detected plane
            modelNode?.let { node ->
                node.position = Position(
                    anchor.pose.tx(),
                    anchor.pose.ty(),
                    anchor.pose.tz()
                )
                arSceneView.addChildNode(node)
                // Optionally, remove the onSessionUpdated callback if you only want to place the model once
                arSceneView.onSessionUpdated = null
            }
        }
    }
}
