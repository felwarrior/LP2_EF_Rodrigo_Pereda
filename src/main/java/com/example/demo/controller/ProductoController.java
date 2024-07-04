package com.example.demo.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.entity.CategoriaEntity;
import com.example.demo.entity.ProductoEntity;
import com.example.demo.entity.UsuarioEntity;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.ProductoRepository;
import com.example.demo.service.ProductoService;
import com.example.demo.service.UsuarioService;
import com.example.demo.service.impl.PdfService;

@Controller
public class ProductoController {

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private ProductoService productoService;

	@Autowired
	private CategoriaRepository categoriaRepository;
	
	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private PdfService pdfService;

	@GetMapping("/menu")
	public String showMenu(HttpSession session, Model model) {
		if (session.getAttribute("usuario") == null) {
			return "redirect:/";
		}

		String correo = session.getAttribute("usuario").toString();
		UsuarioEntity usuarioEntity = usuarioService.buscarUsuarioPorCorreo(correo);
		model.addAttribute("foto", usuarioEntity.getUrlImagen());
		model.addAttribute("nombre", usuarioEntity.getNombre());
		model.addAttribute("apellidos", usuarioEntity.getApellidos());

		List<ProductoEntity> productos = productoService.buscarTodosProductos();
		model.addAttribute("productos", productos);

		return "menu";
	}

	@GetMapping("/nuevo_producto")
	public String showRegistrarProducto(HttpSession session, Model model) {

		String correo = session.getAttribute("usuario").toString();
		UsuarioEntity usuarioEntity = usuarioService.buscarUsuarioPorCorreo(correo);
		model.addAttribute("foto", usuarioEntity.getUrlImagen());
		model.addAttribute("nombre", usuarioEntity.getNombre());
		model.addAttribute("apellidos", usuarioEntity.getApellidos());

		model.addAttribute("producto", new ProductoEntity());
		List<CategoriaEntity> categorias = categoriaRepository.findAll();
		if (categorias.isEmpty()) {
			CategoriaEntity categoriaLacteos = new CategoriaEntity(null, "Lacteos");
			CategoriaEntity categoriaEmbutidos = new CategoriaEntity(null, "Embutidos");
			CategoriaEntity categoriaAbarrotes = new CategoriaEntity(null, "Abarrotes");
			categoriaRepository.saveAll(List.of(categoriaLacteos, categoriaEmbutidos, categoriaAbarrotes));
			categorias = List.of(categoriaLacteos, categoriaEmbutidos, categoriaAbarrotes);
		}
		model.addAttribute("categorias", categorias);
		return "registrar_producto";
	}

	@PostMapping("/nuevo_producto")
	public String registrarProducto(@ModelAttribute ProductoEntity producto, Model model) {
		CategoriaEntity categoria = categoriaRepository.findById(producto.getCategoriaEntity().getCategoria_id())
				.orElseThrow(() -> new IllegalArgumentException("Categoría no válida"));
		producto.setCategoriaEntity(categoria);
		productoService.save(producto);
		return "redirect:/menu";
	}
	
	@GetMapping("/detalle_producto/{id}")
	public String verProducto(HttpSession session, Model model, @PathVariable("id") Long id) {
		
		String correo = session.getAttribute("usuario").toString();
		UsuarioEntity usuarioEntity = usuarioService.buscarUsuarioPorCorreo(correo);
		model.addAttribute("foto", usuarioEntity.getUrlImagen());
		model.addAttribute("nombre", usuarioEntity.getNombre());
		model.addAttribute("apellidos", usuarioEntity.getApellidos());
		
	    ProductoEntity productoEncontrado = productoService.buscarProductoPorId(id);
	    model.addAttribute("producto", productoEncontrado);
	    return "detalle_producto";
	}
	
	@GetMapping("/eliminar_producto/{id}")
	public String eliminarProducto(@PathVariable("id") Long id) {
	    productoRepository.deleteById(id);
	    return "redirect:/menu";
	}
	
	@GetMapping("/editar_producto/{id}")
    public String mostrarFormularioEditar(HttpSession session, @PathVariable("id") Long id, Model model) {

		String correo = session.getAttribute("usuario").toString();
		UsuarioEntity usuarioEntity = usuarioService.buscarUsuarioPorCorreo(correo);
		model.addAttribute("foto", usuarioEntity.getUrlImagen());
		model.addAttribute("nombre", usuarioEntity.getNombre());
		model.addAttribute("apellidos", usuarioEntity.getApellidos());
		
		ProductoEntity producto = productoService.buscarProductoPorId(id);
		List<CategoriaEntity> categorias = categoriaRepository.findAll();
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", categorias);
        return "editar_producto";
    }

    @PostMapping("/actualizar_producto")
    public String actualizarProducto(@ModelAttribute("producto") ProductoEntity producto, Model model) {
        try {
            productoService.save(producto);
            return "redirect:/menu";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Error al actualizar el producto: " + e.getMessage());
            model.addAttribute("producto", producto);
            return "editar_producto";
        }
    }
    
    @GetMapping("/productos/generar_pdf")
    public ResponseEntity<InputStreamResource> generarPdf(HttpSession session, Model model) throws IOException {

		String correo = session.getAttribute("usuario").toString();
		UsuarioEntity usuarioEntity = usuarioService.buscarUsuarioPorCorreo(correo);
		model.addAttribute("nombre", usuarioEntity.getNombre());
		model.addAttribute("apellidos", usuarioEntity.getApellidos());
		
		String nombre = usuarioEntity.getNombre();
		String apellido = usuarioEntity.getApellidos();
		
		String nombreCompletoUsuario = nombre+" "+apellido;
		
    	List<ProductoEntity> productos = productoService.buscarTodosProductos();
        
        
        Map<String, Object> datosPdf = Map.of(
            "productos", productos,
            "nombreCompletoUsuario", nombreCompletoUsuario
            
        );

        ByteArrayInputStream pdfBytes = pdfService.generarPdfDeHtml("template_pdf_productos", datosPdf);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Disposition", "inline; filename=productos.pdf");

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfBytes));
    }

}
