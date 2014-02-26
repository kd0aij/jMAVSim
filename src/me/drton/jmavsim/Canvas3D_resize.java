package me.drton.jmavsim;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;

import javax.media.j3d.Canvas3D;
import javax.swing.JFrame;

public class Canvas3D_resize  implements java.awt.event.ComponentListener {
  JFrame adaptee;

  public Canvas3D_resize(JFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void componentHidden(ComponentEvent e) {
  }
  public void componentMoved(ComponentEvent e) {
  }
  public void componentResized(ComponentEvent e) {
    // resize the canvas3D
    Component[] comp = adaptee.getContentPane().getComponents();
    Canvas3D canvas = (Canvas3D)comp[0];
    Dimension fDim = adaptee.getSize();
    canvas.setBounds(0, 0, fDim.width, fDim.height);
  }
  public void componentShown(ComponentEvent e) {
  }
}
