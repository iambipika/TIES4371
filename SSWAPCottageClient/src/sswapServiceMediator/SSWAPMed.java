package sswapServiceMediator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import info.sswap.api.model.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import info.sswap.api.http.HTTPProvider;
import org.openjena.atlas.iterator.Iter;

public class SSWAPMed {

	private String serviceUrl = "";
	private String queryResult;

	public SSWAPMed(){

System.out.println("-------------");	
System.out.println("I am Mediator...");	
System.out.println("-------------");	
	}

	public void sendRequest(String str) {		
		serviceUrl = str;
		System.out.println("Service URL: " + serviceUrl);
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(serviceUrl);
		CloseableHttpResponse response = null;
		try {
			response = client.execute(httpGet);
		} catch (Exception e) {
			System.out.println("Error executing httpGet: " + e);
		}

		RDG rdg = null;
		try {
			URI uri = new URI(serviceUrl);
			rdg = SSWAP.getResourceGraph(response.getEntity().getContent(), RDG.class, uri);
		} catch (DataAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SSWAPResource resource = rdg.getResource();


		System.out.println("Resource name: " + resource.getName());
		System.out.println("Resource oneline description: " + resource.getOneLineDescription());
		
		readRDG(rdg);

		try {
			client.close();
			response.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	public SSWAPSubject getSubject(RDG rdg) {
		SSWAPResource resource = rdg.getResource();
		SSWAPGraph graph = resource.getGraph();
		SSWAPSubject subject = graph.getSubject();
		return subject;
	}
	
	
	public void readRDG(final RDG rdg) {
		System.out.println("---");
		System.out.println("Read RDG...");
		System.out.println("---");
		SSWAPSubject subject = getSubject(rdg);

		Iterator<SSWAPProperty> iterator = subject.getProperties().iterator();
		System.out.println("Request properties:");
		while (iterator.hasNext()) { // this is a string property
			SSWAPProperty property = iterator.next();
			if (property.getValue().isLiteral()) {
				String lookupName = getStrName(property.getURI());
				System.out.println(""+lookupName);
			} else if (property.getValue().isIndividual()) { // this is an object property
				SSWAPIndividual individual = property.getValue().asIndividual();
//				Iterator<SSWAPType> types = individual.getDeclaredTypes().iterator();
				Iterator<SSWAPProperty> indIterator = individual.getProperties().iterator();
				while (indIterator.hasNext()) {
					SSWAPProperty indProperty = indIterator.next();
					if(indProperty.getValue().isLiteral()){
						String lookupName = getStrName(indProperty.getURI());
						System.out.println(""+lookupName);
					}else if(indProperty.getValue().isIndividual()){
						// we suppose there are no individuals in nested property
						System.out.println("Nested property value is Individual:");
 					}else{System.out.println("Nested property value is ???");}
				}
			}
		}
//		return
		sendRIG(rdg);
   }

	/**
	 * Sends the RIG to the service to get the RRG.
	 * @param rdg the RDG where the RIG is taken from.
	 * @return the service result.
	 */
	public void sendRIG(RDG rdg) {	
		System.out.println("---");
		System.out.println("Send RIG...");
		System.out.println("---");
		boolean errors = false;
		SSWAPSubject subject = getSubject(rdg);
		SSWAPResource resource = rdg.getResource();

		Iterator<SSWAPProperty> iterator = subject.getProperties().iterator();

		String lookupName = "";
		String lookupValue = "";
		int i=0;

		while (iterator.hasNext()) { // this is a string property
			SSWAPProperty property = iterator.next();
			if (property.getValue().isLiteral()) {
				SSWAPPredicate predicate = rdg.getPredicate(property.getURI());
				i++;
				subject.setProperty(predicate, "value"+i);
			} else if (property.getValue().isIndividual()) { // this is an object property
				SSWAPIndividual individual = property.getValue().asIndividual();
				Iterator<SSWAPProperty> indIterator = individual.getProperties().iterator();
				while (indIterator.hasNext()) {
					SSWAPProperty indProperty = indIterator.next();
					if(indProperty.getValue().isLiteral()){
						SSWAPPredicate predicate = rdg.getPredicate(indProperty.getURI());
						i++;
						individual.setProperty(predicate, "value"+i);
					}else if(indProperty.getValue().isIndividual()){
						// we suppose there are no individuals in nested property
						System.out.println("Nested property value is Individual:");
					}else{System.out.println("Nested property value is ???");}
				}
			}
		}
		
		iterator = subject.getProperties().iterator();
		while (iterator.hasNext()) {
			SSWAPProperty property = iterator.next();
			if (property.getValue().isLiteral()) {
				SSWAPPredicate predicate = rdg.getPredicate(property.getURI());
				lookupName = getStrName(property.getURI());
				lookupValue = getStrValue(subject,predicate);
				System.out.println(""+lookupName+" : "+lookupValue);
			} else if (property.getValue().isIndividual()) { // this is an object property
				SSWAPIndividual individual = property.getValue().asIndividual();
				Iterator<SSWAPProperty> indIterator = individual.getProperties().iterator();
				while (indIterator.hasNext()) {
					SSWAPProperty indProperty = indIterator.next();
					if(indProperty.getValue().isLiteral()){
						SSWAPPredicate predicate = rdg.getPredicate(indProperty.getURI());
						lookupName = getStrName(indProperty.getURI());
						lookupValue = getStrValue(individual,predicate);
						System.out.println(""+lookupName+" : "+lookupValue);
					}else if(indProperty.getValue().isIndividual()){
						// we suppose there are no individuals in nested property
						System.out.println("Nested property value is Individual:");
					}else{System.out.println("Nested property value is ???");}
				}
			}
		}

		if (errors) return;

		SSWAPGraph graph = resource.getGraph();
		graph.setSubject(subject);
		resource.setGraph(graph);

		RIG rig = resource.getRDG().getRIG();
		HTTPProvider.RRGResponse response = rig.invoke(10 * 60 * 1000);//increase timeout to 5 mins
		RRG rrg = response.getRRG();

		showResults(rrg);
	}


	/**
	 * Shows the results from the RRG graph 
	 * @param rrg the RRG graph where the results are taken from
	 */
	public void showResults(RRG rrg) {		
		System.out.println("---");
		System.out.println("Get RRG...");
		System.out.println("---");
		SSWAPResource resource = rrg.getResource();
		SSWAPGraph graph = resource.getGraph();
		SSWAPSubject subject = graph.getSubject();
		Iterator<SSWAPObject> iteratorObjects =  subject.getObjects().iterator();
		int i = 1;
		while (iteratorObjects.hasNext()) {
			SSWAPObject object = iteratorObjects.next();
			System.out.println("Result: "+i+" -------------");
			Iterator<SSWAPProperty> iteratorProperties = object.getProperties().iterator();
			while (iteratorProperties.hasNext()) {
				SSWAPProperty property = iteratorProperties.next();
				if (property.getValue().isLiteral()) {
					SSWAPPredicate predicate = rrg.getPredicate(property.getURI());
					String lookupName = getStrName(property.getURI());
					String lookupValue = getStrValue(object,predicate);
					System.out.println(""+lookupName+" : "+lookupValue);
				} else if (property.getValue().isIndividual()) {
					SSWAPIndividual individual = property.getValue().asIndividual();
					Iterator<SSWAPProperty> indIterator = individual.getProperties().iterator();
					while (indIterator.hasNext()) {
						SSWAPProperty indProperty = indIterator.next();
						if(indProperty.getValue().isLiteral()){
							SSWAPPredicate predicate = rrg.getPredicate(indProperty.getURI());
							String lookupName = getStrName(indProperty.getURI());
							String lookupValue = getStrValue(individual,predicate);
							System.out.println(""+lookupName+" : "+lookupValue);
						} else if(indProperty.getValue().isIndividual()){
							// we suppose there are no individuals in nested property
							System.out.println("Nested property value is Individual:");
						}else{System.out.println("Nested property value is ???");}
					}
				}

			}
			i++;

		}
		
	}


	/**
	 * Gets the value of a property from an individual
	 * @param sswapIndividual the individual whose property it is
	 * @param sswapPredicate the predicate of the property
	 * @return the value of the property as String
	 */
	private String getStrValue(SSWAPIndividual sswapIndividual, SSWAPPredicate sswapPredicate) {
		String value = null;
		SSWAPProperty sswapProperty = sswapIndividual.getProperty(sswapPredicate);
		if ( sswapProperty != null ) {
			value = sswapProperty.getValue().asString();
			if ( value.isEmpty() ) {
				value = null;
			}
		}
		return value;
	}

	/**
	 * Gets the name of the property
	 * @param uri uri of the property
	 * @return name of the property
	 */
	private String getStrName(URI uri) {
		String[] parts = uri.toString().split("#");
		return parts[1];
	}
	
	public String getResult(){
		queryResult = "Done!";
		return queryResult;
	}
	
	
}
