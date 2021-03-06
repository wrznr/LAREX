package com.web.controller;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.web.communication.FullBookResponse;
import com.web.communication.SegmentationRequest;
import com.web.communication.SegmentationResult;
import com.web.communication.SegmentationStatus;
import com.web.facade.LarexFacade;
import com.web.model.Book;
import com.web.model.BookSegmentation;
import com.web.model.BookSettings;
import com.web.model.Polygon;
import com.web.model.database.FileDatabase;
import com.web.model.database.IDatabase;

import larex.regions.type.RegionType;
import larex.segmentation.parameters.ImageSegType;

/**
 * Communication Controller to handle requests for the main viewer/editor.
 * Handles requests about displaying book scans and segmentations.
 * 
 */
@Controller
@Scope("request")
public class ViewerController {
	@Autowired
	private ServletContext servletContext;
	@Autowired
	private LarexFacade segmenter;
	@Autowired
	private FileManager fileManager;
	
	@RequestMapping(value = "/viewer", method = RequestMethod.GET)
	public String viewer(Model model, @RequestParam(value = "book", required = false) Integer bookID) throws IOException {
		if(!fileManager.isInit()){
			fileManager.init(servletContext);
		}
		if(bookID == null){
			return "redirect:/404";
		}
		
		segmenter.clear();
		prepareSegmenter(bookID);
		Book book = segmenter.getBook();
		
		if(book == null){
			return "redirect:/404";
		}
		
		model.addAttribute("book", book);
		model.addAttribute("segmenttypes", getSegmentTypes());
		model.addAttribute("imageSegTypes",getImageSegmentTypes());
		model.addAttribute("bookPath", fileManager.getWebBooksPath());
			
		return "editor";
	}

	@RequestMapping(value = "/book", method = RequestMethod.POST)
	public @ResponseBody FullBookResponse getBook(@RequestParam("bookid") int bookID, @RequestParam("pageid") int pageID ) {
		prepareSegmenter(bookID);
		Book book = segmenter.getBook();
		BookSettings settings = segmenter.getDefaultSettings(book);
		BookSegmentation segmentation = segmenter.segmentPage(settings, pageID);

		FullBookResponse bookview = new FullBookResponse(book, segmentation, settings);
		return bookview;
	}

	@RequestMapping(value = "/segment", method = RequestMethod.POST, headers = "Accept=*/*", produces = "application/json", consumes = "application/json")
	public @ResponseBody SegmentationResult segment(@RequestBody SegmentationRequest segmentationRequest) {
		
		BookSegmentation segmentation = segmenter.segmentPages(segmentationRequest.getSettings(), segmentationRequest.getPages());
		SegmentationResult result = new SegmentationResult(segmentation, SegmentationStatus.SUCCESS);
		return result;
	}

	@RequestMapping(value = "/merge", method = RequestMethod.POST)
	public @ResponseBody Polygon segment(@RequestParam("segmentids[]") List<String> segmentIDs, @RequestParam("pageid") int pageID) {
		Polygon merged = segmenter.merge(segmentIDs, pageID);
		return merged;
	}
	
	private LarexFacade prepareSegmenter(int bookID) {
		if(!fileManager.isInit()){
			fileManager.init(servletContext);
		}
		IDatabase database = new FileDatabase(new File(fileManager.getBooksPath()));

		if (!segmenter.isInit()) {
			String resourcepath = fileManager.getBooksPath();
			segmenter.init(database.getBook(bookID), resourcepath);
		} else if (bookID != segmenter.getBook().getId()) {
			segmenter.setBook(database.getBook(bookID));
		}
		return segmenter;
	}

	private Map<RegionType, Integer> getSegmentTypes() {
		//Comparator<RegionType> compareAlphabetically = (RegionType o1, RegionType o2)->o1.toString().compareTo(o2.toString());
		Comparator<RegionType> compareAlphabetically = new Comparator<RegionType>() {
			@Override
			public int compare(RegionType o1, RegionType o2) {
				return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
			}
		};
		Map<RegionType, Integer> segmentTypes = new TreeMap<RegionType, Integer>(compareAlphabetically);

		int i = 0;
		for (RegionType type : RegionType.values()) {
			segmentTypes.put(type, i);
			i++;
		}
		return segmentTypes;
	}
	
	private Map<ImageSegType, String> getImageSegmentTypes() {
		//Comparator<ImageSegType> compareAlphabetically = (ImageSegType o1, ImageSegType o2)->o1.toString().compareTo(o2.toString());
		Map<ImageSegType, String> segmentTypes = new TreeMap<ImageSegType, String>();
		segmentTypes.put(ImageSegType.NONE, "None");
		segmentTypes.put(ImageSegType.CONTOUR_ONLY, "Contour only");
		segmentTypes.put(ImageSegType.STRAIGHT_RECT, "Straight rectangle");
		segmentTypes.put(ImageSegType.ROTATED_RECT, "Rotated rectangle");
		/*int i = 0;
		for (ImageSegType type : ImageSegType.values()) {
			segmentTypes.put(type, i);
			i++;
		}*/
		return segmentTypes;
	}
}
