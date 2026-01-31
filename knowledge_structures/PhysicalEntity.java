package knowledge;

import java.util.ArrayList;

public class PhysicalEntity extends Logos {
	
	// An Entity with physical form
	
	public FormDescription physicalForm = new FormDescription();
	public ArrayList<PropertyScalar> physicalProperties = new ArrayList<PropertyScalar>();
	public RelativePosition currentPosition = new RelativePosition();
	public TimeMoment creationTime = new TimeMoment();
	public TimeMoment destructionTime = new TimeMoment();
	public ArrayList<PhysicalEntity> components = new ArrayList<PhysicalEntity>();
	

}
