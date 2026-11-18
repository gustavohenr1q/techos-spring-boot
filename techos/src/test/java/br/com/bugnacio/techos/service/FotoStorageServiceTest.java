
package br.com.bugnacio.techos.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FotoStorageServiceTest {

    @TempDir
    Path pastaTemporaria;

    private FotoStorageService criarService() {
        return new FotoStorageService(
                pastaTemporaria.resolve("fotos").toString()
        );
    }

    private byte[] imagemPng() throws Exception {

        BufferedImage imagem = new BufferedImage(
                20,
                20,
                BufferedImage.TYPE_INT_RGB
        );

        ByteArrayOutputStream saida =
                new ByteArrayOutputStream();

        assertTrue(ImageIO.write(imagem, "png", saida));

        return saida.toByteArray();
    }

    @Test
    void deveSalvarERecuperarFoto() throws Exception {

        FotoStorageService service = criarService();

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "teste.png",
                "image/png",
                imagemPng()
        );

        FotoStorageService.FotoSalva salva =
                service.salvar(arquivo);

        assertNotNull(salva.nomeArquivo());
        assertEquals("image/png", salva.contentType());

        byte[] recuperada =
                service.ler(salva.nomeArquivo());

        assertTrue(recuperada.length > 0);
    }

    @Test
    void naoDeveAceitarArquivoDeTexto() {

        FotoStorageService service = criarService();

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "foto.png",
                "image/png",
                "Isto não é uma imagem".getBytes()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.salvar(arquivo)
        );
    }

    @Test
    void naoDeveAceitarArquivoVazio() {

        FotoStorageService service = criarService();

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "foto.png",
                "image/png",
                new byte[0]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.salvar(arquivo)
        );
    }

    @Test
    void naoDeveAceitarFotoMaiorQueCincoMB() {

        FotoStorageService service = criarService();

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "foto.png",
                "image/png",
                new byte[5 * 1024 * 1024 + 1]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.salvar(arquivo)
        );
    }

    @Test
    void naoDevePermitirCaminhoInvalido() {

        FotoStorageService service = criarService();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.ler("../segredo.txt")
        );
    }
}
