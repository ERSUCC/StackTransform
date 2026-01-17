package org.ersucc.stacktransform

import ij.{ IJ, Macro, WindowManager }
import ij.gui.{ GenericDialog, Line, NonBlockingGenericDialog }
import ij.plugin.PlugIn

import java.awt.event.{ ActionEvent, KeyAdapter, KeyEvent }

import scala.math.{ atan2, toDegrees }

class StackTransform extends PlugIn {
  override def run(arg: String): Unit = {
    if (WindowManager.getImageCount() == 0)
      return IJ.error("Stack Transform", "No images open.")

    val dialog = new GenericDialog("Stack Transform")

    dialog.addImageChoice("Select image/stack to align:", null)
    dialog.showDialog()

    if (dialog.wasOKed) {
      val image = dialog.getNextImage()
      val stack = image.getImageStack()
      val slices = stack.size()

      val width = image.getWidth()
      val height = image.getHeight()

      var refAngle = 0.0
      var refX = 0
      var refY = 0

      val canvas = image.getWindow().getCanvas()

      for (i <- 1 to slices) {
        image.setSlice(i)

        val processor = stack.getProcessor(i)

        var line: Option[Line] = None

        while (line.isEmpty) {
          val nextDialog = new NonBlockingGenericDialog("Stack Transform")

          nextDialog.addMessage("Draw a line on the current slice.")
          nextDialog.setAlwaysOnTop(true)

          val keyListener = new KeyAdapter {
            override def keyPressed(e: KeyEvent): Unit = {
              if (e.getKeyCode == KeyEvent.VK_ENTER) {
                val button = nextDialog.getButtons()(0)

                button.dispatchEvent(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, button.getName))
              }
            }
          }

          canvas.addKeyListener(keyListener)

          nextDialog.showDialog()

          canvas.removeKeyListener(keyListener)

          if (nextDialog.wasOKed) {
            image.getRoi() match {
              case l: Line =>
                line = Option(l)

              case _ =>
                IJ.error("Stack Transform", "No line is drawn on the current slice.")
            }
          } else {
            throw new RuntimeException(Macro.MACRO_CANCELED)
          }
        }

        line.foreach { l =>
          val bounds = l.getBounds()
          val angle = toDegrees(atan2(-bounds.height, bounds.width))
          val cx = bounds.x + bounds.width / 2
          val cy = bounds.y + bounds.height / 2

          if (i == 1) {
            refAngle = angle
            refX = cx
            refY = cy
          } else {
            processor.translate(width / 2 - cx, height / 2 - cy)
            processor.rotate(angle - refAngle)
            processor.translate(refX - width / 2, refY - height / 2)
          }
        }
      }

      image.resetRoi()
      image.updateAndDraw()
    }
  }
}
