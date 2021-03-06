package com.web.controller;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.web.model.Library;
import com.web.model.database.FileDatabase;
import com.web.model.database.IDatabase;

/**
 * Communication Controller to handle simple requests about the book library.
 * 
 */
@Controller
@Scope("request")
public class LibraryController {
	@Autowired
	private ServletContext servletContext;
	@Autowired
	private FileManager fileController;

	@RequestMapping(value = "/")
	public String home(Model model) throws IOException {
		if(!fileController.isInit()){
			fileController.init(servletContext);
		}
		
		File bookPath = new File(fileController.getBooksPath());
		bookPath.isDirectory();
		IDatabase database = new FileDatabase(bookPath);
		Library lib = new Library(database);

		model.addAttribute("library", lib);
		return "lib";
	}
}
