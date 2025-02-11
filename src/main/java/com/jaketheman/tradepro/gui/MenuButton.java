package com.jaketheman.tradepro.gui;

import com.jaketheman.tradepro.util.ItemFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MenuButton {

  private MenuAction action;
  private ItemFactory icon;
}
