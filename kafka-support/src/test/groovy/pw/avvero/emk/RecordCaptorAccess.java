package pw.avvero.emk;

import java.util.List;

public interface RecordCaptorAccess {

    List<Object> getRecords(String topic, Object key);
}
