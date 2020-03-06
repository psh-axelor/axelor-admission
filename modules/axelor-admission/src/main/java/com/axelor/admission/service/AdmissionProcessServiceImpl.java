package com.axelor.admission.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.axelor.admission.db.AdmissionEntry;
import com.axelor.admission.db.AdmissionProcess;
import com.axelor.admission.db.CollegeEntry;
import com.axelor.admission.db.Faculty;
import com.axelor.admission.db.FacultyEntry;
import com.axelor.admission.db.repo.AdmissionEntryRepository;
import com.axelor.admission.db.repo.FacultyEntryRepository;
import com.axelor.admission.db.repo.FacultyRepository;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;

public class AdmissionProcessServiceImpl implements AdmissionProcessService {
	@Transactional
	@Override
	public void completeAdmission(AdmissionProcess admissionProcess) {
		LocalDate fromDate = admissionProcess.getFromDate();
		LocalDate toDate = admissionProcess.getToDate();
		 BigDecimal merit,merrit1;
		 LocalDate regDate,regDate1;

		List<Faculty> facultyList = Beans.get(FacultyRepository.class).all().fetch();

		if (!facultyList.isEmpty() && facultyList != null) {
			for (Faculty faculty : facultyList) {
				List<AdmissionEntry> admissionEntryList = Beans.get(AdmissionEntryRepository.class).all().filter(
						"self.status=2 AND self.faculty=? AND self.registrationDate>=? AND self.registrationDate <= ?",
						faculty.getId(), fromDate, toDate).fetch();
				
				  AdmissionEntry admissionEntryArray[] = new AdmissionEntry[admissionEntryList.size()]; 
			        for (int j = 0; j < admissionEntryList.size(); j++) { 
			            admissionEntryArray[j] = admissionEntryList.get(j); 
			        } 
			   
				for (int i = 0; i < admissionEntryArray.length; i++) {
					for (int j = i+1; j < admissionEntryArray.length; j++) {
						if(admissionEntryArray[i].getMerit().intValue()<admissionEntryArray[j].getMerit().intValue())
						{
							merit=admissionEntryArray[i].getMerit();
							merrit1=admissionEntryArray[j].getMerit();
							admissionEntryArray[i].getMerit().add(merrit1.subtract(merit));
							admissionEntryArray[j].getMerit().add(merit.subtract(merrit1));
						}
						else if(admissionEntryArray[i].getMerit().intValue()==admissionEntryArray[j].getMerit().intValue())
						{
							if(!admissionEntryArray[j].getRegistrationDate().isAfter(admissionEntryArray[i].getRegistrationDate()))
							{
								AdmissionEntry admission = admissionEntryArray[i];
								admissionEntryArray[i]=admissionEntryArray[j];
								admissionEntryArray[j]=admission;
							}
						}
						
					}
				}
				List<AdmissionEntry> admissionEntrySortedList = new ArrayList<AdmissionEntry>();
				admissionEntrySortedList = Arrays.asList(admissionEntryArray);

				
				for (AdmissionEntry admissionEntry : admissionEntrySortedList) {
					for (CollegeEntry collegeEntry : admissionEntry.getCollegesList()) {
						FacultyEntry facultyEntry = Beans.get(FacultyEntryRepository.class).all()
								.filter("self.faculty=? AND self.college=?", faculty.getId(), collegeEntry.getCollege())
								.fetchOne();

						int avilableSeat = facultyEntry.getSeats();

						if (avilableSeat > 0) {
							admissionEntry.setCollegesSelected(collegeEntry.getCollege());
							admissionEntry.setValidationDate(LocalDate.now());
							admissionEntry.setStatus(3);
							avilableSeat--;
							facultyEntry.setSeats(avilableSeat);
						}
					}
					if (admissionEntry.getStatus() == 2) {
						admissionEntry.setStatus(4);
						admissionEntry.setValidationDate(null);
						admissionEntry.setCollegesSelected(null);
					}
				}
			}
		}
	}
}
