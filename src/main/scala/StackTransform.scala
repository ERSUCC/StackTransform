package org.ersucc.stacktransform

import ij.{ IJ, Macro, WindowManager }
import ij.gui.{ GenericDialog, Line, NonBlockingGenericDialog }
import ij.plugin.PlugIn
import ij.process.Blitter

import java.awt.event.{ ActionEvent, KeyAdapter, KeyEvent }

class StackTransform extends PlugIn {
  override def run(arg: String): Unit = {
    if (WindowManager.getImageCount() == 0)
      return IJ.error("Stack Transform", "No images open.")

    val dialog = new GenericDialog("Stack Transform")

    dialog.addImageChoice("Select stack to align:", null)
    dialog.addChoice("Select stack for second channel:", "--" +: WindowManager.getImageTitles(), null)
    dialog.addChoice("Select stack for third channel:", "--" +: WindowManager.getImageTitles(), null)
    dialog.showDialog()

    if (dialog.wasOKed) {
      val images = Array(dialog.getNextChoice(), dialog.getNextChoice(), dialog.getNextChoice()).distinct.collect {
        case name if name != "--" =>
          WindowManager.getImage(name)
      }

      val stacks = images.map(_.getImageStack())
      val slices = stacks(0).size()

      if (stacks.tail.exists(_.size() != slices))
        return IJ.error("Stack Transform", "The selected stacks do not have the same number of slices.")

      val width = images(0).getWidth()
      val height = images(0).getHeight()

      var refAngle = 0.0
      var refX = 0
      var refY = 0

      val canvas = images(0).getWindow().getCanvas()

      for (i <- 1 to slices) {
        images.foreach(_.setSlice(i))

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
            images(0).getRoi() match {
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
          val angle = l.getAngle()
          val bounds = l.getBounds()
          val cx = bounds.x + bounds.width / 2
          val cy = bounds.y + bounds.height / 2

          if (i == 1) {
            refAngle = angle
            refX = cx
            refY = cy
          } else {
            stacks.map(_.getProcessor(i)).foreach { processor =>
              val buffer = processor.createProcessor(width * 2, height * 2)

              buffer.copyBits(processor, width / 2, height / 2, Blitter.COPY)
              buffer.translate(width / 2 - cx, height / 2 - cy)
              buffer.rotate(angle - refAngle)
              buffer.translate(refX - width / 2, refY - height / 2)

              processor.copyBits(buffer, -width / 2, -height / 2, Blitter.COPY)
            }
          }
        }
      }

      images.foreach(_.updateAndDraw())
    }
  }
}
