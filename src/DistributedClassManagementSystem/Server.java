package DistributedClassManagementSystem;

import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

class CenterServerImpl extends CenterServerPOA {

	private ORB orb;

	private volatile HashMap<String, ArrayList<Record>> recordData;

	private ArrayList<Record> tempRecords;

	private String name;

	private int serverPort;

	private HashMap<String, Integer> nodePorts;

	private volatile boolean isServerRunning;

	public CenterServerImpl(String name, int serverPort, HashMap<String, Integer> nodePorts) {
		super();

		LoggerFactory.Log(name, "Initialing Center");
		this.name = name;
		recordData = new HashMap<String, ArrayList<Record>>();
		this.serverPort = serverPort;
		this.nodePorts = nodePorts;
		this.isServerRunning = true;
		this.tempRecords = new ArrayList<Record>();

		new Thread(new Runnable() {

			@Override
			public void run() {
				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket(serverPort);
					LoggerFactory.Log(name, "UDP server Started in " + name + " region in this port " + serverPort);
					byte[] buffer = new byte[1000];
					while (isServerRunning) {
						DatagramPacket request = new DatagramPacket(buffer, buffer.length);
						socket.receive(request);
						LoggerFactory.Log(name,
								"Received request in " + name + " from " + request.getAddress() + ":"
										+ request.getPort() + " with this data "
										+ new String(request.getData()).replaceAll("\u0000.*", "") + "");
						String replyData = "";
						String requestData = new String(request.getData()).replaceAll("\u0000.*", "");
						if (requestData.startsWith("GET_RECORD_COUNT")) {
							replyData = getRecordCount(name + "_SERVER");
						} else if (requestData.startsWith("TRANSFER_TEACHER")) {

							replyData = processRecordTransferRequest(requestData, "Teacher");
						} else if (requestData.startsWith("ADD_TEACHER")) {
							replyData = processAddTransferRequest(requestData, "Teacher");
						} else if (requestData.startsWith("TRANSFER_STUDENT")) {

							replyData = processRecordTransferRequest(requestData, "Student");
						} else if (requestData.startsWith("ADD_STUDENT")) {
							replyData = processAddTransferRequest(requestData, "Student");
						}

						else {
							replyData = "INVALID_REQUEST";
						}
						DatagramPacket reply = new DatagramPacket(replyData.getBytes(), replyData.length(),
								request.getAddress(), request.getPort());
						socket.send(reply);
					}

				} catch (Exception e) {
					System.out.println(e);
					LoggerFactory.Log(name, "Unable to start udp server in " + name + " region");
				}

			}
		}).start();
		LoggerFactory.Log(name, "Center initialed");

	}

	private String processRecordTransferRequest(String requestData, String type) {
		requestData = requestData.replaceAll("TRANSFER_" + type.toUpperCase() + ";", "");
		if(type.equalsIgnoreCase("Teacher"))
		{
		TeacherRecord teacher = TeacherRecord.fromString(requestData);
		tempRecords.add(teacher);
		}
		else
		{
			StudentRecord student = StudentRecord.fromString(requestData);
			tempRecords.add(student);
		}
		return "OK";
	}
	
	

	private String processAddTransferRequest(String requestData, String type) {
		LoggerFactory.Log(name, "Processing ADD_" + type.toUpperCase() + " request");

		requestData = requestData.replaceAll("ADD_" + type.toUpperCase()  + ";", "");
		String[] str = requestData.split(";");
		String managerID = str[0];
		String recordID = str[1];
		LoggerFactory.Log(name, "Processing ADD_" + type.toUpperCase() + " request, managerID:" + managerID + " recordID:" + recordID);
		Record record = null;

		for (Record r : tempRecords) {

			if (r.getRecordId().equalsIgnoreCase(recordID.trim())) {
				record = r;
				break;
			}
		}

		if (record == null) {

			LoggerFactory.Log(name, "Record not found");
			return "Record_not_found";
		} else {
			
			LoggerFactory.Log(name, "Record in temp list found, " + record.toString());
			
			if(type.equalsIgnoreCase("Teacher"))
			{
			TeacherRecord teacher = (TeacherRecord)record;
			createTRecord(teacher.getFirstName(), teacher.getLastName(), teacher.getAddress(), teacher.getPhone(),
					teacher.getSpecialization(), teacher.getLocation(), managerID);
			}
			else
			{
				StudentRecord student = (StudentRecord)record;
				this.createSRecord(student.getFirstName(), student.getLastName(), student.getCoursesRegistered(), student.getStatus(), student.getStatusDate(), managerID);
			}
			
			
			LoggerFactory.Log(name, "Attemping to remove record from temp list");

			tempRecords.remove(record);

			LoggerFactory.Log(name, "Record removed from temp list");
			return "OK";
		}
	}

	public void setORB(ORB orb) {
		this.orb = orb;
	}

	@Override
	public boolean createTRecord(String firstName, String lastName, String address, String phone, String specialization,
			String location, String managerId) {

		LoggerFactory.Log(this.name, "Creating Teacher Record.");
		LoggerFactory.Log(this.name, "Validating fields...");
		if (firstName == null || firstName.isEmpty()) {
			LoggerFactory.Log(this.name, "First name required");
			// throw new RequiredValueException("First name required");
		}

		if (lastName == null || lastName.isEmpty()) {
			LoggerFactory.Log(this.name, "Last name required");
			// throw new RequiredValueException("Last name required");
		}

		if (address == null || address.isEmpty()) {
			LoggerFactory.Log(this.name, "Address required");
			// throw new RequiredValueException("Address required");
		}

		if (phone == null || phone.isEmpty()) {
			LoggerFactory.Log(this.name, "Phone required");
			// throw new RequiredValueException("Phone required");
		}

		if (specialization == null || specialization.isEmpty()) {
			LoggerFactory.Log(this.name, "Specialization required");
			// throw new RequiredValueException("Specialization required");
		}

		if (location == null) {
			LoggerFactory.Log(this.name, "Status required");
			// throw new RequiredValueException("Status required");
		}
		LoggerFactory.Log(this.name, "Validating fields complete...");
		Record record = new TeacherRecord("TR" + generateNumber(), firstName, lastName, address, phone, specialization,
				location);

		String firstCharacter = record.getLastName().substring(0, 1).toUpperCase();

		LoggerFactory.Log(this.name, "Adding Record data to List...");
		Boolean result = addToRecordData(firstCharacter, record);

		if (result) {
			LoggerFactory.Log(this.name, String.format("Record added to the list :%s", record.toString()));
			LoggerFactory.Log(this.name,
					String.format("Teacher Record Successfully created by Manager:%s", (managerId)));
		} else {
			LoggerFactory.Log(this.name,
					String.format("Something went wrong when creating teacher record :%s \n by Manager: %s",
							record.toString(), (managerId)));
		}

		return result;
	}

	@Override
	public boolean createSRecord(String firstName, String lastName, String[] courseRegistered, String status,
			String statusDate, String managerId) {

		LoggerFactory.Log(this.name, "Creating Student Record...");
		LoggerFactory.Log(this.name, "Validating fields...");
		if (firstName == null || firstName.isEmpty()) {
			LoggerFactory.Log(this.name, "First name required");
			// throw new RequiredValueException("First name required");
		}

		if (lastName == null || lastName.isEmpty()) {
			LoggerFactory.Log(this.name, "Last name required");
			// throw new RequiredValueException("Last name required");
		}

		if (statusDate == null || statusDate.isEmpty()) {
			LoggerFactory.Log(this.name, "Status Date required");
			// throw new RequiredValueException("Status Date required");
		}

		if (courseRegistered == null || courseRegistered.length < 1) {
			LoggerFactory.Log(this.name, "Registed Course required");
			// throw new RequiredValueException("Registered Course required");
		}

		if (status == null) {
			LoggerFactory.Log(this.name, "Status required");
			// throw new RequiredValueException("Status required");
		}
		LoggerFactory.Log(this.name, "Validating fields complete...");

		Record record = new StudentRecord("SR" + generateNumber(), firstName, lastName, courseRegistered, status,
				statusDate);

		String firstCharacter = record.getLastName().substring(0, 1).toUpperCase();

		LoggerFactory.Log(this.name, "Adding Record data to List...");
		boolean result = addToRecordData(firstCharacter, record);

		if (result) {
			LoggerFactory.Log(this.name, String.format("Record added to the list :%s", record.toString()));
			LoggerFactory.Log(this.name,
					String.format("Student Record Successfully created by Manager:%s", (managerId)));
		} else {
			LoggerFactory.Log(this.name,
					String.format("Something went wrong when creating student record :%s \n by Manager: %s",
							record.toString(), (managerId)));
		}
		return result;
	}

	@Override
	public String getRecordCount(String managerId) {

		if (!(managerId.equalsIgnoreCase("MTL_SERVER") || managerId.equalsIgnoreCase("LVL_SERVER")
				|| managerId.equalsIgnoreCase("DDO_SERVER"))) {

			LoggerFactory.Log(this.name,
					"Received request for " + this.name + " server from " + managerId + " to get record counts.");

			Set<String> keys = this.recordData.keySet();
			int count = 0;
			for (String key : keys) {
				if (this.recordData.get(key) != null) {
					count += this.recordData.get(key).size();
				}
			}

			String recordCountData = this.name + ": " + count;
			LoggerFactory.Log(this.name, "Total Records in " + this.name + " server are " + recordCountData);

			int[] nodeports = new int[2];
			int i = 0;
			for (Entry<String, Integer> node : this.nodePorts.entrySet())
				nodeports[i++] = node.getValue();

			final HashMap<Integer, String> result = new HashMap<Integer, String>() {
				{
					put(nodeports[0], "");
					put(nodeports[1], "");
				}
			};

			final CountDownLatch latch = new CountDownLatch(2);
			for (int port : nodeports) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						DatagramSocket socket = null;
						try {
							socket = new DatagramSocket();
							InetAddress host = InetAddress.getLocalHost();
							byte[] requestData = "GET_RECORD_COUNT".getBytes();
							DatagramPacket request = new DatagramPacket(requestData, requestData.length, host, port);
							socket.send(request);
							LoggerFactory.Log(name,
									"Request sent to get record data from " + host.getHostName() + ":" + port);
							byte[] buffer = new byte[1000];
							DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
							socket.receive(reply);
							String replyData = new String(buffer).replaceAll("\u0000.*", "");
							LoggerFactory.Log(name,
									"Received this response " + replyData + " from " + host.getHostName() + ":" + port);
							if (!replyData.equals("INVALID_REQUEST")) {
								result.put(port, replyData);
							}
						} catch (SocketException e) {
							System.out.println(e);
							LoggerFactory.Log(name, "Error occur to connect another region server");
						} catch (UnknownHostException e) {
							System.out.println(e);
							LoggerFactory.Log(name, "Invalid host");
						} catch (IOException e) {
							System.out.println(e);
							LoggerFactory.Log(name, "Invalid data");
						} finally {
							if (socket != null) {
								socket.close();
							}
						}
						latch.countDown();
					}
				}).start();

			}
			try {
				latch.await();
				recordCountData += " " + result.get(nodeports[0]) + " " + result.get(nodeports[1]);
			} catch (InterruptedException e) {

			}

			return recordCountData;
		} else {
			Set<String> keys = this.recordData.keySet();
			int count = 0;
			for (String key : keys) {
				if (this.recordData.get(key) != null) {
					count += this.recordData.get(key).size();
				}
			}

			return this.name + ": " + count;
		}
	}

	private Record findRecord(String recordId) {
		LoggerFactory.Log(this.name, "Looking record id");
		Record record = null;
		for (ArrayList<Record> records : this.recordData.values()) {

			for (Record r : records) {

				if (r.getRecordId().equalsIgnoreCase(recordId)) {

					record = r;
					break;
				}
			}

			if (record != null)
				break;
		}

		return record;

	}

	@Override
	public boolean editRecords(String recordId, String fieldName, String newValue, String managerId) {

		LoggerFactory.Log(this.name, "Manager :" + managerId + " requested to edit a record.");
		LoggerFactory.Log(this.name, String.format("Editing record, RecordID:%s", recordId));

		if (recordId == null || recordId.isEmpty()) {
			LoggerFactory.Log(this.name, "Record ID required");
			// throw new RequiredValueException("Record ID required");
		}

		if (fieldName == null || fieldName.isEmpty()) {
			LoggerFactory.Log(this.name, "FieldName required");
			// throw new RequiredValueException("FieldName required");
		}

		if (newValue == null || newValue.isEmpty()) {
			LoggerFactory.Log(this.name, "FieldValue required");
			// throw new RequiredValueException("FieldValue required");
		}

		Record record = findRecord(recordId);

		if (record == null) {
			LoggerFactory.Log(this.name, String.format("Record not found, %s", recordId));
			return false;
		}

		synchronized (record.getLock()) {

			record = findRecord(recordId);
			if (record == null) {
				LoggerFactory.Log(this.name, String.format("Record not found, %s", recordId));
				return false;
			}

			LoggerFactory.Log(this.name, String.format("Record found, %s", record.toString()));

			if (record.getClass() == StudentRecord.class) {
				StudentRecord student = (StudentRecord) record;
				switch (fieldName.toLowerCase()) {
				case "firstname":
					student.setFirstName(newValue);
					break;
				case "lastname":
					student.setLastName(newValue);
					break;
				case "status":

					if (!newValue.toLowerCase().equals("active") && !newValue.toLowerCase().equals("inactive")) {
						LoggerFactory.Log(this.name, "Status is invalid");
						// throw new RequiredValueException("Status is invalid");
					}
					student.setStatus(newValue.toLowerCase());
					break;
				case "statusdate":
					try {
						DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
						Date today = df.parse(newValue);
						student.setStatusDate(newValue);
					} catch (ParseException e) {
						LoggerFactory.Log(this.name, "Date is invalid");
						// throw new RequiredValueException("Date is invalid");
					}
					break;
				case "coursesregistered":
					student.setCoursesRegistered(newValue.split(","));
					break;
				default:
					LoggerFactory.Log(this.name, "FieldName is invalid");
					// throw new RequiredValueException("FieldName is invalid");
				}

				LoggerFactory.Log(this.name, String.format("Student record edited, %s", student));

			} else {
				TeacherRecord teacher = (TeacherRecord) record;
				switch (fieldName.toLowerCase()) {
				case "firstname":
					assert teacher != null;
					teacher.setFirstName(newValue);
					break;
				case "lastname":
					assert teacher != null;
					teacher.setLastName(newValue);
					break;
				case "address":
					assert teacher != null;
					teacher.setAddress(newValue);
					break;
				case "phone":
					assert teacher != null;
					teacher.setPhone(newValue);
					break;
				case "specialization":
					assert teacher != null;
					teacher.setSpecialization(newValue);
					break;

				case "location":

					if (!newValue.toLowerCase().equals("mtl") && !newValue.toLowerCase().equals("lvl")
							&& !newValue.toLowerCase().equals("ddo")) {
						LoggerFactory.Log(this.name, "location is invalid");
						// throw new RequiredValueException("location is invalid");
					}
					assert teacher != null;
					teacher.setLocation(newValue.toLowerCase());
					break;
				default:
					LoggerFactory.Log(this.name, "FieldName is invalid");
					// throw new RequiredValueException("FieldName is invalid");
				}

				LoggerFactory.Log(this.name, String.format("Teacher record edited, %s", teacher));
			}
		}

		return true;
	}

	private void removeRecord(Record record) {
		for (ArrayList<Record> records : recordData.values()) {

			for (Record r : records) {

				if (r.getRecordId().equalsIgnoreCase(record.getRecordId())) {

					records.remove(r);
					return;
				}
			}
		}
	}

	private String sendSocketRequest(DatagramSocket socket, String requestString, InetAddress host, int port,
			String remoteServer) throws IOException {
		LoggerFactory.Log(name, "Sending request to center " + remoteServer + " request details:" + requestString);
		byte[] requestData = requestString.getBytes();
		DatagramPacket request = new DatagramPacket(requestData, requestData.length, host, port);
		socket.send(request);
		LoggerFactory.Log(name, "Request sent to transfer teacher record data to " + host.getHostName() + ":" + port);
		byte[] buffer = new byte[1000];
		DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
		socket.receive(reply);
		String replyData = new String(buffer).replaceAll("\u0000.*", "");
		LoggerFactory.Log(name, "Received this response " + replyData + " from " + host.getHostName() + ":" + port);

		return replyData;
	}

	private boolean transferTeacher(TeacherRecord teacher, String managerID, int port, String remoteServer) {

		final CountDownLatch latch = new CountDownLatch(1);

		new Thread(new Runnable() {

			@Override
			public void run() {
				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket();
					InetAddress host = InetAddress.getLocalHost();
					tempRecords.add(teacher);
					LoggerFactory.Log(name, Integer.toString(port));

					String replyData = sendSocketRequest(socket, "TRANSFER_TEACHER;" + teacher.toSplited(), host, port,
							remoteServer);

					if (!replyData.equalsIgnoreCase("OK")) {
						LoggerFactory.Log(name, "Error occur to transfer teacher");
						tempRecords.remove(teacher);
					} else {
						removeRecord(teacher);
						LoggerFactory.Log(name, "Teacher record removed");

						replyData = sendSocketRequest(socket,
								"ADD_TEACHER;" + managerID + ";" + teacher.getRecordId() + ";", host, port,
								remoteServer);

						if (!replyData.equalsIgnoreCase("OK")) {
							LoggerFactory.Log(name, "Error occur to add teacher");
							String firstCharacter = teacher.getLastName().substring(0, 1).toUpperCase();
							addToRecordData(firstCharacter, teacher);
							tempRecords.remove(teacher);
						} else {
							tempRecords.remove(teacher);
						}

					}

				} catch (SocketException e) {
					System.out.println(e);
					LoggerFactory.Log(name, "Error occur to connect another region server");

				} catch (UnknownHostException e) {
					System.out.println(e);
					LoggerFactory.Log(name, "Invalid host");
				} catch (IOException e) {
					System.out.println(e);
					LoggerFactory.Log(name, "Invalid data");
				} finally {
					if (socket != null) {
						socket.close();
					}

					latch.countDown();
				}

			}
		}).start();

		try {
			latch.await();
			return true;
		} catch (InterruptedException e) {

			return false;
		}

	}

	private boolean transferRecord(Record record, String type, String managerID, int port, String remoteServer) {

		final CountDownLatch latch = new CountDownLatch(1);

		new Thread(new Runnable() {

			@Override
			public void run() {
				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket();
					InetAddress host = InetAddress.getLocalHost();
					tempRecords.add(record);
					LoggerFactory.Log(name, Integer.toString(port));

					String replyData = sendSocketRequest(socket,
							"TRANSFER_" + type.toUpperCase() + ";" + record.toSplited(), host, port, remoteServer);

					if (!replyData.equalsIgnoreCase("OK")) {
						LoggerFactory.Log(name, "Error occur to transfer teacher");
						tempRecords.remove(record);
					} else {
						removeRecord(record);
						LoggerFactory.Log(name, type + " record removed");

						replyData = sendSocketRequest(socket,
								"ADD_" + type.toUpperCase() + ";" + managerID + ";" + record.getRecordId() + ";", host,
								port, remoteServer);

						if (!replyData.equalsIgnoreCase("OK")) {
							LoggerFactory.Log(name, "Error occur to add " + type);
							String firstCharacter = record.getLastName().substring(0, 1).toUpperCase();
							addToRecordData(firstCharacter, record);
							tempRecords.remove(record);
						} else {
							tempRecords.remove(record);
						}

					}

				} catch (SocketException e) {
					System.out.println(e);
					LoggerFactory.Log(name, "Error occur to connect another region server");

				} catch (UnknownHostException e) {
					System.out.println(e);
					LoggerFactory.Log(name, "Invalid host");
				} catch (IOException e) {
					System.out.println(e);
					LoggerFactory.Log(name, "Invalid data");
				} finally {
					if (socket != null) {
						socket.close();
					}

					latch.countDown();
				}

			}
		}).start();

		try {
			latch.await();
			return true;
		} catch (InterruptedException e) {

			return false;
		}

	}

	@Override
	public boolean transferRecord(String managerID, String recordID, String remoteCenterServerName) {

		LoggerFactory.Log(this.name, "Manager :" + managerID + " requested to transfer a record.");
		LoggerFactory.Log(this.name, String.format("Transering record, RecordID:%s", recordID));

		Record record = findRecord(recordID);

		if (record == null) {
			LoggerFactory.Log(this.name, String.format("Record not found, %s", recordID));
			return false;
		}

		synchronized (record.getLock()) {

			record = findRecord(recordID);
			if (record == null) {
				LoggerFactory.Log(this.name, String.format("Record not found, %s", recordID));
				return false;
			}

			LoggerFactory.Log(this.name, String.format("Record found, %s", record.toString()));

			int port = this.nodePorts.get(remoteCenterServerName);

			if (record.getClass() == StudentRecord.class) {
				StudentRecord student = (StudentRecord) record;
				transferRecord(student, "Student", managerID, port, remoteCenterServerName);

			} else {
				TeacherRecord teacher = (TeacherRecord) record;
				transferRecord(teacher, "Teacher", managerID, port, remoteCenterServerName);

			}
		}

		return true;
	}

	private synchronized int generateNumber() {

		Random random = new Random(System.nanoTime());

		return 10000 + random.nextInt(89999);
	}

	private boolean addToRecordData(String firstCharacter, Record record) {
		if (this.recordData.containsKey(firstCharacter)) {
			ArrayList<Record> list = this.recordData.get(firstCharacter);
			if (list != null && list.size() > 0) {
				list.add(record);
				return true;
			} else {
				list.add(record);
				this.recordData.put(firstCharacter, list);
				return true;
			}
		} else {
			ArrayList<Record> list = new ArrayList<Record>();
			list.add(record);
			this.recordData.put(firstCharacter, list);
			return true;
		}
	}
}

public class Server {

	private CenterServer mtlRef;
	private CenterServer lvlRef;
	private CenterServer ddoRef;

	private void addStudentsToServer()
			throws IOException, ParseException, RequiredValueException, org.json.simple.parser.ParseException {

		JSONParser parser = new JSONParser();
		JSONArray jsonArray = (JSONArray) parser.parse(new FileReader("resources/studentData.json"));
		for (Object object : jsonArray) {
			String status = "";
			JSONObject student = (JSONObject) object;
			String firstName = (String) student.get("firstName");
			String lastName = (String) student.get("lastName");
			String[] courses = student.get("coursesRegistered").toString().replace("},{", " ,").split(" ");
			String statusDate = (String) student.get("statusDate");
			String stat = (String) student.get("status");
			if (Objects.equals(stat, "Active")) {
				status = "ACTIVE";
			} else {
				status = "INACTIVE";
			}

			if (student.get("region").toString().substring(0, 3).equalsIgnoreCase("MTL")) {
				this.mtlRef.createSRecord(firstName, lastName, courses, status, statusDate, "default");
			} else if (student.get("region").toString().substring(0, 3).equalsIgnoreCase("LVL")) {
				this.lvlRef.createSRecord(firstName, lastName, courses, status, statusDate, "default");
			} else if (student.get("region").toString().substring(0, 3).equalsIgnoreCase("DDO")) {
				this.ddoRef.createSRecord(firstName, lastName, courses, status, statusDate, "default");
			}
		}
	}

	private void addTeachersToServer()
			throws IOException, ParseException, RequiredValueException, org.json.simple.parser.ParseException {

		JSONParser parser = new JSONParser();
		JSONArray jsonArray = (JSONArray) parser.parse(new FileReader("resources/teacherData.json"));
		for (Object object : jsonArray) {
			String location = "";
			JSONObject teacher = (JSONObject) object;
			String firstName = (String) teacher.get("firstName");
			String lastName = (String) teacher.get("lastName");
			String recordId = (String) teacher.get("id");
			String address = (String) teacher.get("address");
			String loc = (String) teacher.get("location");
			String phone = (String) teacher.get("phone");
			String specialization = (String) teacher.get("specialization");

			if (loc.equalsIgnoreCase("MLT")) {
				location = "MTL";
			} else if (loc.equalsIgnoreCase("LVL")) {
				location = "LVL";
			} else {
				location = "DDO";
			}

			if (teacher.get("location").toString().substring(0, 3).equalsIgnoreCase("MTL")) {
				this.mtlRef.createTRecord(firstName, lastName, address, phone, specialization, location, "default");
			} else if (teacher.get("location").toString().substring(0, 3).equalsIgnoreCase("LVL")) {
				this.lvlRef.createTRecord(firstName, lastName, address, phone, specialization, location, "default");
			} else if (teacher.get("location").toString().substring(0, 3).equalsIgnoreCase("DDO")) {
				this.ddoRef.createTRecord(firstName, lastName, address, phone, specialization, location, "default");
			}
		}
	}

	private void startServer(String[] args) {
		try {
			ORB orb = ORB.init(args, null);

			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();

			HashMap<String, Integer> mtlPorts = new HashMap<String, Integer>() {
				{
					put("LVL", 6798);
					put("DDO", 6799);
				}
			};

			CenterServerImpl mtlServer = new CenterServerImpl("MTL", 6797, mtlPorts);
			mtlServer.setORB(orb);
			org.omg.CORBA.Object mtlref = rootpoa.servant_to_reference(mtlServer);
			this.mtlRef = CenterServerHelper.narrow(mtlref);

			HashMap<String, Integer> lvlPorts = new HashMap<String, Integer>() {
				{
					put("MTL", 6797);
					put("DDO", 6799);
				}
			};

			CenterServerImpl lvlServer = new CenterServerImpl("LVL", 6798, lvlPorts);
			lvlServer.setORB(orb);
			org.omg.CORBA.Object lvlref = rootpoa.servant_to_reference(lvlServer);
			this.lvlRef = CenterServerHelper.narrow(lvlref);

			HashMap<String, Integer> ddoPorts = new HashMap<String, Integer>() {
				{
					put("MTL", 6797);
					put("LVL", 6798);
				}
			};

			CenterServerImpl ddoServer = new CenterServerImpl("DDO", 6799, ddoPorts);
			ddoServer.setORB(orb);
			org.omg.CORBA.Object ddoref = rootpoa.servant_to_reference(ddoServer);
			this.ddoRef = CenterServerHelper.narrow(ddoref);

			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

			String name = "MTL";
			NameComponent mtlpath[] = ncRef.to_name(name);
			ncRef.rebind(mtlpath, this.mtlRef);

			name = "LVL";
			NameComponent lvlpath[] = ncRef.to_name(name);
			ncRef.rebind(lvlpath, this.lvlRef);

			name = "DDO";
			NameComponent ddopath[] = ncRef.to_name(name);
			ncRef.rebind(ddopath, this.ddoRef);

			try {
				this.addTeachersToServer();
				this.addStudentsToServer();
			} catch (Exception e) {

			}

			System.out.println("MTL ready and waiting ...");
			System.out.println("LVL ready and waiting ...");
			System.out.println("DDO ready and waiting ...");

			// wait for invocations from clients
			orb.run();
		} catch (Exception e) {
			System.err.println("ERROR: " + e);
			e.printStackTrace(System.out);
		}
	}

	public static void main(String args[]) {
		Server server = new Server();

		server.startServer(args);
	}
}
