package io.xoana.redshift.gameobjects

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.environment.PointLight

// TODO: This is barely even a thin wrapper.  Maybe we should get rid of Environment and all that stuff since we're packing the shader anyway.
class LightObject(var color:Color, var intensity:Float, name:String = "LightObject") : GameObject(name)