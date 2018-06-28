package cl.equifax.kpiservices.bbekpiservices.controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import cl.equifax.kpiservices.bbekpiservices.domain.EndPoint;
import cl.equifax.kpiservices.bbekpiservices.entities.FilesIndex;
import cl.equifax.kpiservices.bbekpiservices.entities.PageDetail;
import cl.equifax.kpiservices.bbekpiservices.libs.Paginator;
import cl.equifax.kpiservices.bbekpiservices.services.FilesIndexService;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping(EndPoint.VERSION_1 + "/filesindex")
public class FilesIndexController {

	private FilesIndexService service;

	@Autowired
	public FilesIndexController(FilesIndexService service) {
		this.service = service;
	}

	@ApiOperation(value = "Obtiene la lista paginada filtradas de archivos")
	@GetMapping
	@ResponseBody
	public PageDetail home(@RequestParam(required = false) String filter, @RequestParam(required = false) String kpi,
			@RequestParam(defaultValue = "0", required = false) Integer page,
			@RequestParam(defaultValue = "10", required = false) Integer size) {

		Pageable pageable = PageRequest.of(page, size);

		if (filter != null && !filter.isEmpty() && kpi != null && !kpi.isEmpty()) {
			Page<FilesIndex> result = this.service.findByKpiAndFilePath(kpi, filter, pageable);
			return Paginator.buildSerializablePage(result);
		}

		if (filter != null && !filter.isEmpty()) {
			Page<FilesIndex> result = this.service.findByFilePath(filter, pageable);
			return Paginator.buildSerializablePage(result);
		}

		if (kpi != null && !kpi.isEmpty()) {
			Page<FilesIndex> result = this.service.findByKpi(kpi, pageable);
			return Paginator.buildSerializablePage(result);
		}

		Page<FilesIndex> result = this.service.findAll(pageable);
		return Paginator.buildSerializablePage(result);
	}

	@ApiOperation(value = "Obtiene la lista actualizada de archivos e índices")
	@GetMapping("/all")
	@ResponseBody
	public List<FilesIndex> getAll() {
		return this.service.findAll();
	}

	@ApiOperation(value = "Obtiene el archivo correspondiente a una subida determinada")
	@GetMapping("/download")
	@ResponseBody
	public ResponseEntity<ByteArrayResource> download(@RequestParam Integer id) throws IOException {
		Optional<FilesIndex> fileIndexOptional = this.service.findById(id);

		if (!fileIndexOptional.isPresent()) {
			throw new FileNotFoundException("File do not exists");
		}

		FilesIndex filesIndex = fileIndexOptional.get();

		String filePath = filesIndex.getFilePath();

		File file = new File(filePath);

		Path path = Paths.get(file.getAbsolutePath());
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
				.contentLength(file.length()).contentType(MediaType.TEXT_PLAIN).body(resource);

	}

	@ApiOperation(value = "Obtiene la lista de archivos e índices de un kpi")
	@GetMapping("/{kpi}")
	@ResponseBody
	public List<FilesIndex> getByKpi(@PathVariable(value = "kpi") String kpi) {
		return this.service.findByKpi(kpi);
	}

	@ApiOperation(value = "Almacena los datos de archivo e índice para un una subida de archivo determinada")
	@PostMapping
	@ResponseBody
	public ResponseEntity<FilesIndex> create(@Valid @RequestBody FilesIndex kpiStructure) {

		FilesIndex savedStructure = service.save(kpiStructure);

		return new ResponseEntity<>(savedStructure, HttpStatus.CREATED);
	}

	@ApiOperation(value = "Elimina un dato de archivo e índice.  Al eliminar ya no se puede utilizar el índice.")
	@DeleteMapping("/{id}")
	public @ResponseBody ResponseEntity<?> delete(@PathVariable(value = "id") Integer id) {

		this.service.delete(id);

		return new ResponseEntity<>(HttpStatus.OK);

	}

}
