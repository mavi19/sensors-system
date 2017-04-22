package configuration;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import model.LightSensor;
import model.StreetLamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import thread.LightSensorThread;
import thread.StreetLampThread;
import util.MappingThreadsToLamps;
import util.MappingThreadsToLightSensors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SensorsConfiguration {

	public final static int LAMPS_FOR_THREAD = 10;
	
	@Autowired
	StreetLampRepository streetLampRepository;
	@Autowired
	LightSensorRepository lightSensorRepository;
	
	public void InitSensorsDB() throws IOException, ParseException{
		
		streetLampRepository.deleteAll();
		lightSensorRepository.deleteAll();
    	
        ObjectMapper mapper = new ObjectMapper();
        List<StreetLamp> streetLampList = mapper.readValue(new File("data/dataset.json"), new TypeReference<List<StreetLamp>>(){});
		
        for(StreetLamp sl: streetLampList){
			streetLampRepository.save(sl);
			
	 		LightSensor lightSensor = new LightSensor();
	 		lightSensor.setLightSensorId(sl.getLampId());
	 		lightSensor.setLightIntensity(0);
	 		lightSensor.setAddress(sl.getAddress());
	 		lightSensor.setTimestamp(0);
	 		lightSensorRepository.save(lightSensor);
 		
			if(sl.getLampId() == 5)//to test with less lamp
				break;
		}
	}
	
	@PostConstruct
	public void initThreadList() {
		
		//comment this try-catch to re-use same DB of last execution
		try {
			InitSensorsDB();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		List<StreetLamp> streetLampList = null;
		streetLampList = streetLampRepository.findAll();
		
		List<LightSensor> sensorLightList = null;
		sensorLightList = lightSensorRepository.findAll();

		for(int i=0; i<streetLampList.size(); i+=LAMPS_FOR_THREAD){
			StreetLampThread lampThread = new StreetLampThread();
			LightSensorThread lightThread = new LightSensorThread();	
			for(int j=0; j<LAMPS_FOR_THREAD && i+j <= streetLampList.size()-1; j++){
    			MappingThreadsToLamps.getInstance().put(streetLampList.get(i+j).getLampId(), lampThread);
     			lampThread.getListAdjustment().getMappingAdjustmentToLamps().put(streetLampList.get(i+j).getLampId(), 0.0);
				lampThread.getStreetLampList().add(streetLampList.get(i+j));
				
				MappingThreadsToLightSensors.getInstance().put(sensorLightList.get(i+j).getLightSensorId(), lightThread);
				lightThread.getLightSensorList().add(sensorLightList.get(i+j));
			}
			lampThread.start();
			lightThread.start();
		}
	}
	
	@PreDestroy
	public void destroyThreadList() {
	
		HashMap<Long, StreetLampThread> tmp = MappingThreadsToLamps.getInstance();
		for (Object value : tmp.values()){
			StreetLampThread t = (StreetLampThread) value;
			if(t.isInterrupted())
				continue;
			t.interrupt();
		}
		
		HashMap<Long, LightSensorThread> tmps = MappingThreadsToLightSensors.getInstance();
		for (Object value : tmps.values()){
			LightSensorThread ts = (LightSensorThread) value;
			if(ts.isInterrupted())
				continue;
			ts.interrupt();
		}
		
	}
	
}
