package gqserver.ui.server.table.model;

import gqserver.api.ServerClient;
import gqserver.core.earthquake.data.Earthquake;
import gqserver.ui.server.table.Column;
import gqserver.ui.server.table.TableCellRendererAdapter;

import java.util.List;

public class EarthquakeTableModel extends FilterableTableModel<Earthquake>{
    private final List<Column<Earthquake, ?>> columns = List.of(
            Column.readonly("lat", Double.class, Earthquake::getLat, new TableCellRendererAdapter<>()),
            Column.readonly("lon", Double.class, Earthquake::getLon, new TableCellRendererAdapter<>()));


    public EarthquakeTableModel(List<Earthquake> data) {
        super(data);
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns.get(columnIndex).getName();
    }

    public TableCellRendererAdapter<?, ?> getColumnRenderer(int columnIndex) {
        return columns.get(columnIndex).getRenderer();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columns.get(columnIndex).getColumnType();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columns.get(columnIndex).isEditable();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Earthquake event = getEntity(rowIndex);
        return columns.get(columnIndex).getValue(event);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Earthquake event = getEntity(rowIndex);
        columns.get(columnIndex).setValue(value, event);
    }
}
