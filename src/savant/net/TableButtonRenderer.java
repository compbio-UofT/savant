/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.net;

import java.awt.Component;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class TableButtonRenderer extends AbstractCellEditor
  implements TableCellRenderer, TableCellEditor
{
  private Map<String, JButton> renderButtons = new WeakHashMap<String, JButton>();

  public Component getTableCellRendererComponent(JTable table, Object value,
    boolean isSelected, boolean hasFocus, int row, int column)
  {
    JButton button = (JButton)value;
    JButton renderButton = renderButtons.get(button.getText());

    if (renderButton == null)
    {
      renderButton = new JButton(button.getText());
      renderButtons.put(button.getText(), renderButton);
    }

    return renderButton;
  }

  public Object getCellEditorValue()
  {
    return null;
  }

  public Component getTableCellEditorComponent(JTable table, Object value,
    boolean isSelected, int row, int column)
  {
    return (JButton)value;
  }
}
